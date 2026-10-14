package com.omjadon.contractanalyzer.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private static final Duration TOKEN_LIFETIME =
            Duration.ofMinutes(15);

    private static final String INVALID_LINK =
            "Reset link is invalid or expired";

    private final AppUserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordResetMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            AppUserRepository users,
            PasswordResetTokenRepository tokens,
            PasswordResetMailSender mailSender,
            PasswordEncoder passwordEncoder
    ) {
        this.users = users;
        this.tokens = tokens;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void requestReset(String email) {
        if (!mailSender.isAvailable()) {
            throw new IllegalStateException(
                    "Password recovery is currently unavailable"
            );
        }

        String normalizedEmail = normalizeEmail(email);

        AppUser candidate = users.findByEmailIgnoreCase(
                normalizedEmail
        ).orElse(null);

        // The controller will return the same acknowledgement
        // whether or not an account exists.
        if (candidate == null) {
            return;
        }

        AppUser user = users.findByIdForUpdate(
                candidate.id()
        ).orElse(null);

        if (user == null) {
            return;
        }

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(TOKEN_LIFETIME);

        // Issuing a new link invalidates previous links.
        // A mail failure rolls this transaction back.
        tokens.deleteAllForUser(user.id());

        tokens.saveAndFlush(new PasswordResetToken(
                hash(rawToken),
                user.id(),
                createdAt,
                expiresAt
        ));

        mailSender.sendResetLink(
                user.email(),
                rawToken,
                expiresAt
        );
    }

    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword
    ) {
        validateToken(rawToken);
        validatePassword(newPassword);

        String tokenHash = hash(rawToken);

        PasswordResetToken candidate = tokens.findById(
                tokenHash
        ).orElseThrow(PasswordResetService::invalidLink);

        // Always acquire the user lock before the token lock.
        AppUser user = users.findByIdForUpdate(
                candidate.userId()
        ).orElseThrow(PasswordResetService::invalidLink);

        // Re-check after acquiring the lock: another transaction
        // may already have used or replaced this token.
        PasswordResetToken token = tokens.findByHashForUpdate(
                tokenHash
        ).orElseThrow(PasswordResetService::invalidLink);

        Instant now = Instant.now();

        if (!token.userId().equals(user.id())
                || !token.isUsableAt(now)) {
            throw invalidLink();
        }

        user.replacePasswordHash(
                passwordEncoder.encode(newPassword)
        );
        token.consume(now);

        users.saveAndFlush(user);

        // Successful reset invalidates every reset link
        // belonging to this account.
        tokens.deleteAllForUser(user.id());
    }

  static String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException(
                    "Enter a valid email address"
            );
        }

        String value = email.trim().toLowerCase(Locale.ROOT);

        if (value.length() > 320
                || value.indexOf('@') <= 0
                || value.indexOf('@') != value.lastIndexOf('@')
                || value.endsWith("@")
                || value.codePoints().anyMatch(
                        Character::isWhitespace
                )
                || value.codePoints().anyMatch(
                        Character::isISOControl
                )) {
            throw new IllegalArgumentException(
                    "Enter a valid email address"
            );
        }

        return value;
    }

    private static void validateToken(String token) {
        if (token == null
                || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw invalidLink();
        }
    }

    private static void validatePassword(String password) {
        if (password == null
                || password.codePointCount(
                        0, password.length()
                ) < 12
                || password.getBytes(
                        StandardCharsets.UTF_8
                ).length > 72) {
            throw new IllegalArgumentException(
                    "Password must contain at least 12 characters "
                            + "and at most 72 UTF-8 bytes"
            );
        }
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(
                    "Required token hashing algorithm is unavailable"
            );
        }
    }

    private static IllegalArgumentException invalidLink() {
        return new IllegalArgumentException(INVALID_LINK);
    }
}