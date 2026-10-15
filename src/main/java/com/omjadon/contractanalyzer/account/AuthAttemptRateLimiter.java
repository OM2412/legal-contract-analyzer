package com.omjadon.contractanalyzer.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.LongSupplier;
import org.springframework.stereotype.Component;

@Component
public class AuthAttemptRateLimiter {

    private static final long WINDOW_NANOS =
            Duration.ofMinutes(15).toNanos();

    private static final int MAX_BUCKETS = 10_000;

    public enum Operation {
        LOGIN(30, 300),
        REGISTER(10, 100);

        private final int perAddress;
        private final int global;

        Operation(int perAddress, int global) {
            this.perAddress = perAddress;
            this.global = global;
        }
    }

    public record Decision(
            boolean allowed,
            long retryAfterSeconds
    ) {
    }

    private static final class Bucket {
        private final long startedAt;
        private int attempts;

        private Bucket(long startedAt) {
            this.startedAt = startedAt;
        }
    }

    private final Map<String, Bucket> buckets = new HashMap<>();
    private final LongSupplier nanoTime;

    public AuthAttemptRateLimiter() {
        this(System::nanoTime);
    }

    AuthAttemptRateLimiter(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    public synchronized Decision acquire(
            Operation operation,
            String remoteAddress
    ) {
        long now = nanoTime.getAsLong();

        buckets.entrySet().removeIf(entry ->
                now - entry.getValue().startedAt >= WINDOW_NANOS
        );

        String globalKey = operation.name() + ":global";
        String addressKey = operation.name()
                + ":address:" + hash(remoteAddress);

        Bucket global = buckets.get(globalKey);
        Bucket address = buckets.get(addressKey);

        long retryAfter = 0;

        if (global != null
                && global.attempts >= operation.global) {
            retryAfter = remainingSeconds(global, now);
        }

        if (address != null
                && address.attempts >= operation.perAddress) {
            retryAfter = Math.max(
                    retryAfter,
                    remainingSeconds(address, now)
            );
        }

        if (retryAfter > 0) {
            return new Decision(false, retryAfter);
        }

        int newBuckets = (global == null ? 1 : 0)
                + (address == null ? 1 : 0);

        if (buckets.size() + newBuckets > MAX_BUCKETS) {
            return new Decision(false, 60);
        }

        if (global == null) {
            global = new Bucket(now);
            buckets.put(globalKey, global);
        }

        if (address == null) {
            address = new Bucket(now);
            buckets.put(addressKey, address);
        }

        global.attempts++;
        address.attempts++;

        return new Decision(true, 0);
    }

    private static long remainingSeconds(
            Bucket bucket,
            long now
    ) {
        long remaining = WINDOW_NANOS
                - (now - bucket.startedAt);

        return Math.max(
                1,
                (remaining + 999_999_999L) / 1_000_000_000L
        );
    }

    private static String hash(String remoteAddress) {
        String input = remoteAddress == null
                ? "unknown"
                : remoteAddress;

        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(
                    "Required hashing algorithm is unavailable",
                    error
            );
        }
    }
}