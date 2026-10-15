package com.omjadon.contractanalyzer.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class AuthAttemptRateLimiterTest {

    @Test
    void loginAllowsThirtyAttemptsThenRejects() {
        AuthAttemptRateLimiter limiter =
                new AuthAttemptRateLimiter(() -> 0L);

        for (int attempt = 0; attempt < 30; attempt++) {
            assertTrue(limiter.acquire(
                    AuthAttemptRateLimiter.Operation.LOGIN,
                    "192.0.2.1"
            ).allowed());
        }

        AuthAttemptRateLimiter.Decision denied = limiter.acquire(
                AuthAttemptRateLimiter.Operation.LOGIN,
                "192.0.2.1"
        );

        assertFalse(denied.allowed());
        assertEquals(900L, denied.retryAfterSeconds());
    }

    @Test
    void registrationAllowsTenAttemptsThenRejects() {
        AuthAttemptRateLimiter limiter =
                new AuthAttemptRateLimiter(() -> 0L);

        for (int attempt = 0; attempt < 10; attempt++) {
            assertTrue(limiter.acquire(
                    AuthAttemptRateLimiter.Operation.REGISTER,
                    "192.0.2.1"
            ).allowed());
        }

        assertFalse(limiter.acquire(
                AuthAttemptRateLimiter.Operation.REGISTER,
                "192.0.2.1"
        ).allowed());
    }

    @Test
    void differentAddressesHaveIndependentLimits() {
        AuthAttemptRateLimiter limiter =
                new AuthAttemptRateLimiter(() -> 0L);

        for (int attempt = 0; attempt < 30; attempt++) {
            limiter.acquire(
                    AuthAttemptRateLimiter.Operation.LOGIN,
                    "192.0.2.1"
            );
        }

        assertFalse(limiter.acquire(
                AuthAttemptRateLimiter.Operation.LOGIN,
                "192.0.2.1"
        ).allowed());

        assertTrue(limiter.acquire(
                AuthAttemptRateLimiter.Operation.LOGIN,
                "192.0.2.2"
        ).allowed());
    }

    @Test
    void registrationLimitDoesNotConsumeLoginAllowance() {
        AuthAttemptRateLimiter limiter =
                new AuthAttemptRateLimiter(() -> 0L);

        for (int attempt = 0; attempt < 10; attempt++) {
            limiter.acquire(
                    AuthAttemptRateLimiter.Operation.REGISTER,
                    "192.0.2.1"
            );
        }

        assertFalse(limiter.acquire(
                AuthAttemptRateLimiter.Operation.REGISTER,
                "192.0.2.1"
        ).allowed());

        for (int attempt = 0; attempt < 30; attempt++) {
            assertTrue(limiter.acquire(
                    AuthAttemptRateLimiter.Operation.LOGIN,
                    "192.0.2.1"
            ).allowed());
        }

        assertFalse(limiter.acquire(
                AuthAttemptRateLimiter.Operation.LOGIN,
                "192.0.2.1"
        ).allowed());
    }

    @Test
    void retryAfterDecreasesAndWindowExpiresAtBoundary() {
        long[] now = {0L};

        AuthAttemptRateLimiter limiter =
                new AuthAttemptRateLimiter(() -> now[0]);

        for (int attempt = 0; attempt < 30; attempt++) {
            limiter.acquire(
                    AuthAttemptRateLimiter.Operation.LOGIN,
                    "192.0.2.1"
            );
        }

        now[0] = Duration.ofSeconds(1).toNanos();

        assertEquals(
                899L,
                limiter.acquire(
                        AuthAttemptRateLimiter.Operation.LOGIN,
                        "192.0.2.1"
                ).retryAfterSeconds()
        );

        now[0] = Duration.ofMinutes(15).toNanos();

        AuthAttemptRateLimiter.Decision allowed = limiter.acquire(
                AuthAttemptRateLimiter.Operation.LOGIN,
                "192.0.2.1"
        );

        assertTrue(allowed.allowed());
        assertEquals(0L, allowed.retryAfterSeconds());
    }

    @Test
    void globalLoginLimitAppliesAcrossAddresses() {
        AuthAttemptRateLimiter limiter =
                new AuthAttemptRateLimiter(() -> 0L);

        for (int address = 1; address <= 10; address++) {
            for (int attempt = 0; attempt < 30; attempt++) {
                assertTrue(limiter.acquire(
                        AuthAttemptRateLimiter.Operation.LOGIN,
                        "192.0.2." + address
                ).allowed());
            }
        }

        assertFalse(limiter.acquire(
                AuthAttemptRateLimiter.Operation.LOGIN,
                "192.0.2.99"
        ).allowed());
    }

    @Test
    void concurrentRequestsCannotExceedAddressLimit()
            throws Exception {
        AuthAttemptRateLimiter limiter =
                new AuthAttemptRateLimiter(() -> 0L);

        List<Callable<Boolean>> requests = new ArrayList<>();

        for (int attempt = 0; attempt < 100; attempt++) {
            requests.add(() -> limiter.acquire(
                    AuthAttemptRateLimiter.Operation.LOGIN,
                    "192.0.2.1"
            ).allowed());
        }

        int accepted = 0;

        try (var executor = Executors.newFixedThreadPool(8)) {
            for (var result : executor.invokeAll(requests)) {
                if (result.get()) {
                    accepted++;
                }
            }
        }

        assertEquals(30, accepted);
    }
}