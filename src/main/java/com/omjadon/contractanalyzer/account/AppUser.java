package com.omjadon.contractanalyzer.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "credential_version", nullable = false)
    private long credentialVersion;

    protected AppUser() {
        // Required by JPA.
    }

    public AppUser(
            String email,
            String displayName,
            String passwordHash
    ) {
        this.id = UUID.randomUUID();
        this.email = required(email, "email")
                .toLowerCase(Locale.ROOT);
        this.displayName = required(displayName, "displayName");
        this.passwordHash = required(passwordHash, "passwordHash");
        this.createdAt = Instant.now();
        this.credentialVersion = 0;

        if (this.email.length() > 320
                || this.displayName.length() > 120
                || this.passwordHash.length() > 255) {
            throw new IllegalArgumentException(
                    "Account field exceeds database limit"
            );
        }
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    String passwordHash() {
        return passwordHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public long credentialVersion() {
        return credentialVersion;
    }

    /**
     * Accepts an encoded password only.
     * The reset service must validate and encode the new password first.
     */
    void replacePasswordHash(String encodedPassword) {
        String replacement = required(
                encodedPassword,
                "encodedPassword"
        );

        if (replacement.length() > 255) {
            throw new IllegalArgumentException(
                    "Encoded password exceeds database limit"
            );
        }

        long nextVersion = Math.incrementExact(credentialVersion);

        this.passwordHash = replacement;
        this.credentialVersion = nextVersion;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        return value.trim();
    }
}