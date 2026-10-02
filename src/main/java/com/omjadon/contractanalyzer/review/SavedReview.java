package com.omjadon.contractanalyzer.review;

import com.omjadon.contractanalyzer.account.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "saved_reviews")
public class SavedReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(name = "agreement_filename", nullable = false, length = 255)
    private String agreementFilename;

    @Column(name = "sow_filename", nullable = false, length = 255)
    private String sowFilename;

    @Column(name = "agreement_version", nullable = false, length = 71)
    private String agreementVersion;

    @Column(name = "sow_version", nullable = false, length = 71)
    private String sowVersion;

    @Column(name = "policy_max_calendar_days", nullable = false)
    private int policyMaxCalendarDays;

    @Column(name = "review_status", nullable = false, length = 40)
    private String reviewStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> resultJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SavedReview() {
        // Required by JPA.
    }

    public SavedReview(
            AppUser owner,
            String agreementFilename,
            String sowFilename,
            String agreementVersion,
            String sowVersion,
            int policyMaxCalendarDays,
            String reviewStatus,
            Map<String, Object> resultJson
    ) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.agreementFilename = requireText(
                agreementFilename, "agreementFilename"
        );
        this.sowFilename = requireText(sowFilename, "sowFilename");
        this.agreementVersion = requireText(
                agreementVersion, "agreementVersion"
        );
        this.sowVersion = requireText(sowVersion, "sowVersion");

        if (policyMaxCalendarDays < 1 || policyMaxCalendarDays > 3650) {
            throw new IllegalArgumentException(
                    "Policy limit must be between 1 and 3650 days"
            );
        }

        this.policyMaxCalendarDays = policyMaxCalendarDays;
        this.reviewStatus = requireText(reviewStatus, "reviewStatus");
        this.resultJson = new LinkedHashMap<>(
                Objects.requireNonNull(resultJson, "resultJson")
        );
    }

    @PrePersist
    void setCreationTime() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID id() {
        return id;
    }

    public UUID ownerId() {
        return owner.id();
    }

    public String agreementFilename() {
        return agreementFilename;
    }

    public String sowFilename() {
        return sowFilename;
    }

    public String agreementVersion() {
        return agreementVersion;
    }

    public String sowVersion() {
        return sowVersion;
    }

    public int policyMaxCalendarDays() {
        return policyMaxCalendarDays;
    }

    public String reviewStatus() {
        return reviewStatus;
    }

    public Map<String, Object> resultJson() {
        return resultJson;
    }

    public Instant createdAt() {
        return createdAt;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }
        return value;
    }
}