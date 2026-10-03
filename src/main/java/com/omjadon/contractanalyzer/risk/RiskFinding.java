package com.omjadon.contractanalyzer.risk;

import com.omjadon.contractanalyzer.model.EvidenceSpan;

import java.util.List;
import java.util.Objects;

public record RiskFinding(
        String findingId,
        Category category,
        Signal signal,
        Priority priority,
        String title,
        String explanation,
        String ruleId,
        List<EvidenceSpan> evidence
) {
    public enum Category {
        PAYMENT,
        TERMINATION,
        LIABILITY,
        CONFIDENTIALITY,
        INTELLECTUAL_PROPERTY,
        DATA_HANDLING
    }

    public enum Signal {
        POLICY_DEVIATION,
        DOCUMENT_DIFFERENCE,
        AMBIGUOUS_WORDING,
        COVERAGE_GAP,
        CLAUSE_FOR_REVIEW
    }

    public enum Priority {
        REVIEW_ONLY,
        LOW,
        MEDIUM,
        HIGH
    }

    public RiskFinding {
        requireNonBlank(findingId, "findingId");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(signal, "signal");
        Objects.requireNonNull(priority, "priority");
        requireNonBlank(title, "title");
        requireNonBlank(explanation, "explanation");
        requireNonBlank(ruleId, "ruleId");
        Objects.requireNonNull(evidence, "evidence");

        evidence = List.copyOf(evidence);

        if (evidence.isEmpty() && signal != Signal.COVERAGE_GAP) {
            throw new IllegalArgumentException(
                    "A source-backed finding requires evidence"
            );
        }
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " must not be null or blank"
            );
        }
    }
}