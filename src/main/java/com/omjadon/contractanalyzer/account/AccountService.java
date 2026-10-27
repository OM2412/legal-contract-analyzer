package com.omjadon.contractanalyzer.account;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder(12);

    public AccountService(AppUserRepository users) {
        this.users = users;
    }

    public record Account(
            UUID id,
            String email,
            String displayName
    ) {
    }

    @Transactional
    public Account register(
            String email,
            String displayName,
            String password
    ) {
        String normalizedEmail = validateEmail(email);
        String safeName = validateName(displayName);
        validatePassword(password);

        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException(
                    "Email is already registered"
            );
        }

        AppUser user = new AppUser(
                normalizedEmail,
                safeName,
                passwordEncoder.encode(password)
        );

        try {
            AppUser saved = users.saveAndFlush(user);
            return new Account(
                    saved.id(),
                    saved.email(),
                    saved.displayName()
            );
        } catch (DataIntegrityViolationException duplicate) {
            throw new IllegalArgumentException(
                    "Email is already registered",
                    duplicate
            );
        }
    }

    private static String validateEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }

        String value = email.trim().toLowerCase(Locale.ROOT);

        if (value.length() > 320
                || value.indexOf('@') <= 0
                || value.indexOf('@') != value.lastIndexOf('@')
                || value.endsWith("@")
                || value.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "Enter a valid email address"
            );
        }

        return value;
    }

    private static String validateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name is required");
        }

        String value = name.trim();

        if (value.length() < 2 || value.length() > 120) {
            throw new IllegalArgumentException(
                    "Name must contain 2 to 120 characters"
            );
        }

        return value;
    }

    private static void validatePassword(String password) {
        if (password == null
                || password.codePointCount(0, password.length()) < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "Password must contain at least 12 characters "
                            + "and at most 72 UTF-8 bytes"
            );
        }
    }
}