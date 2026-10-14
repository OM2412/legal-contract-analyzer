package com.omjadon.contractanalyzer.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected PasswordResetToken() {
        // Required by JPA.
    }

    public PasswordResetToken(
            String tokenHash,
            UUID userId,
            Instant createdAt,
            Instant expiresAt
    ) {
        if (tokenHash == null
                || !tokenHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "Token hash must contain 64 lowercase hexadecimal characters"
            );
        }

        this.userId = Objects.requireNonNull(userId, "userId");
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt"
        );
        this.expiresAt = Objects.requireNonNull(
                expiresAt,
                "expiresAt"
        );

        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                    "Token expiry must be after creation"
            );
        }

        this.tokenHash = tokenHash;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public UUID userId() {
        return userId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant consumedAt() {
        return consumedAt;
    }

    public boolean isUsableAt(Instant now) {
        Objects.requireNonNull(now, "now");

        return consumedAt == null
                && !now.isBefore(createdAt)
                && now.isBefore(expiresAt);
    }

    void consume(Instant now) {
        if (!isUsableAt(now)) {
            throw new IllegalArgumentException(
                    "Reset link is invalid or expired"
            );
        }

        this.consumedAt = now;
    }
}