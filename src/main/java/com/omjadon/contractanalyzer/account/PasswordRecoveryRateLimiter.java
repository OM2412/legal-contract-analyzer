package com.omjadon.contractanalyzer.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PasswordRecoveryRateLimiter {

    private static final long WINDOW_NANOS =
            Duration.ofMinutes(15).toNanos();

    private static final int MAX_BUCKETS = 10_000;

    private final Map<String, Bucket> buckets = new HashMap<>();

    private static final class Bucket {
        private final long startedAt;
        private int attempts;

        private Bucket(long startedAt) {
            this.startedAt = startedAt;
        }
    }

    public synchronized boolean allowRequest(
            String remoteAddress,
            String email
    ) {
        long now = System.nanoTime();
        removeExpired(now);

        // Count every request, including malformed or unknown emails.
        if (!acquire("request-global", 300, now)) {
            return false;
        }

        if (!acquire(
                "request-ip:" + hash(remoteAddress),
                20,
                now
        )) {
            return false;
        }

        String normalizedEmail = email == null
                ? ""
                : email.trim().toLowerCase(Locale.ROOT);

        return acquire(
                "request-email:" + hash(normalizedEmail),
                3,
                now
        );
    }

    public synchronized boolean allowReset(
            String remoteAddress
    ) {
        long now = System.nanoTime();
        removeExpired(now);

        if (!acquire("reset-global", 300, now)) {
            return false;
        }

        return acquire(
                "reset-ip:" + hash(remoteAddress),
                20,
                now
        );
    }

    private boolean acquire(
            String key,
            int limit,
            long now
    ) {
        Bucket bucket = buckets.get(key);

        if (bucket == null) {
            if (buckets.size() >= MAX_BUCKETS) {
                return false;
            }

            bucket = new Bucket(now);
            buckets.put(key, bucket);
        }

        if (bucket.attempts >= limit) {
            return false;
        }

        bucket.attempts++;
        return true;
    }

    private void removeExpired(long now) {
        buckets.entrySet().removeIf(entry ->
                now - entry.getValue().startedAt >= WINDOW_NANOS
        );
    }

    private static String hash(String value) {
        String input = value == null ? "unknown" : value;

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(
                    "Required hashing algorithm is unavailable"
            );
        }
    }
}