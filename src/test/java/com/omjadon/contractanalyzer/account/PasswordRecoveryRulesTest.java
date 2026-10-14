package com.omjadon.contractanalyzer.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PasswordRecoveryRulesTest {

    private static final String HASH = "a".repeat(64);
    private static final Instant CREATED =
            Instant.parse("2026-10-15T00:00:00Z");
    private static final Instant EXPIRES =
            CREATED.plusSeconds(900);

    private static PasswordResetToken token() {
        return new PasswordResetToken(
                HASH,
                UUID.randomUUID(),
                CREATED,
                EXPIRES
        );
    }

    @Test
    void tokenIsUsableFromCreationUntilBeforeExpiry() {
        PasswordResetToken token = token();

        assertTrue(token.isUsableAt(CREATED));
        assertTrue(token.isUsableAt(EXPIRES.minusNanos(1)));
    }

    @Test
    void tokenIsRejectedAtExpiryAndBeforeCreation() {
        PasswordResetToken token = token();

        assertFalse(token.isUsableAt(CREATED.minusNanos(1)));
        assertFalse(token.isUsableAt(EXPIRES));
        assertFalse(token.isUsableAt(EXPIRES.plusSeconds(1)));

        assertThrows(
                IllegalArgumentException.class,
                () -> token.consume(EXPIRES)
        );
    }

    @Test
    void consumedTokenCannotBeUsedAgain() {
        PasswordResetToken token = token();
        Instant consumedAt = CREATED.plusSeconds(30);

        token.consume(consumedAt);

        assertEquals(consumedAt, token.consumedAt());
        assertFalse(token.isUsableAt(consumedAt));
        assertFalse(token.isUsableAt(consumedAt.plusSeconds(1)));

        assertThrows(
                IllegalArgumentException.class,
                () -> token.consume(consumedAt.plusSeconds(1))
        );
    }

    @Test
    void tokenRequiresValidHashAndPositiveLifetime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PasswordResetToken(
                        "not-a-sha256-hash",
                        UUID.randomUUID(),
                        CREATED,
                        EXPIRES
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PasswordResetToken(
                        HASH,
                        UUID.randomUUID(),
                        CREATED,
                        CREATED
                )
        );
    }

    @Test
    void passwordChangeAdvancesVersionWithoutChangingOldPrincipal() {
        AppUser user = new AppUser(
                "test@example.test",
                "Test User",
                "encoded-old-password"
        );

        AccountPrincipal oldPrincipal = new AccountPrincipal(user);

        user.replacePasswordHash("encoded-new-password");

        assertEquals("encoded-new-password", user.passwordHash());
        assertEquals(1L, user.credentialVersion());
        assertEquals(0L, oldPrincipal.credentialVersion());

        AccountPrincipal newPrincipal = new AccountPrincipal(user);
        assertEquals(1L, newPrincipal.credentialVersion());
        assertEquals(oldPrincipal.userId(), newPrincipal.userId());
    }

    @Test
    void invalidPasswordHashDoesNotAdvanceVersion() {
        AppUser user = new AppUser(
                "test@example.test",
                "Test User",
                "encoded-old-password"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> user.replacePasswordHash(" ")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> user.replacePasswordHash("a".repeat(256))
        );

        assertEquals(0L, user.credentialVersion());
        assertEquals("encoded-old-password", user.passwordHash());
    }

    @Test
    void emailLimitAppliesAcrossAddressesAndEmailCase() {
        PasswordRecoveryRateLimiter limiter =
                new PasswordRecoveryRateLimiter();

        assertTrue(limiter.allowRequest(
                "127.0.0.1", "User@example.test"
        ));
        assertTrue(limiter.allowRequest(
                "127.0.0.2", " user@example.test "
        ));
        assertTrue(limiter.allowRequest(
                "127.0.0.3", "USER@example.test"
        ));

        assertFalse(limiter.allowRequest(
                "127.0.0.4", "user@example.test"
        ));

        assertTrue(limiter.allowRequest(
                "127.0.0.4", "other@example.test"
        ));
    }

    @Test
    void requestAddressLimitAppliesAcrossDifferentEmails() {
        PasswordRecoveryRateLimiter limiter =
                new PasswordRecoveryRateLimiter();

        for (int index = 0; index < 20; index++) {
            assertTrue(limiter.allowRequest(
                    "127.0.0.1",
                    "user-" + index + "@example.test"
            ));
        }

        assertFalse(limiter.allowRequest(
                "127.0.0.1", "another@example.test"
        ));

        assertTrue(limiter.allowRequest(
                "127.0.0.2", "different@example.test"
        ));
    }

    @Test
    void resetAddressLimitRejectsAttemptTwentyOne() {
        PasswordRecoveryRateLimiter limiter =
                new PasswordRecoveryRateLimiter();

        for (int index = 0; index < 20; index++) {
            assertTrue(limiter.allowReset("127.0.0.1"));
        }

        assertFalse(limiter.allowReset("127.0.0.1"));
        assertTrue(limiter.allowReset("127.0.0.2"));
    }
}