package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.EvidenceSpan;

import java.util.Objects;

/**
 * Structured facts extracted from one payment obligation.
 */
public record PaymentTerm(
        int days,
        DayUnit dayUnit,
        PaymentTrigger trigger,
        String scope,
        String payer,
        String payee,
        EvidenceSpan evidence
) {

    public enum DayUnit {
        CALENDAR_DAYS,
        BUSINESS_DAYS,
        UNKNOWN
    }

    public enum PaymentTrigger {
        INVOICE_RECEIPT,
        INVOICE_DATE,
        FINAL_ACCEPTANCE,
        OTHER,
        UNKNOWN
    }

    public PaymentTerm {
        if (days < 0) {
            throw new IllegalArgumentException("days must not be negative");
        }

        Objects.requireNonNull(dayUnit, "dayUnit");
        Objects.requireNonNull(trigger, "trigger");
        Objects.requireNonNull(evidence, "evidence");

        requireNonBlank(scope, "scope");
        requireNonBlank(payer, "payer");
        requireNonBlank(payee, "payee");
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " must not be null or blank"
            );
        }
    }
}