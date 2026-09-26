package com.omjadon.contractanalyzer.payment;

import java.util.Objects;

/**
 * A reviewer's selected business preference for payment timing.
 */
public record PaymentPolicy(
        String policyId,
        String version,
        int maximumCalendarDays,
        PaymentTerm.PaymentTrigger requiredTrigger
) {

    public PaymentPolicy {
        requireNonBlank(policyId, "policyId");
        requireNonBlank(version, "version");
        Objects.requireNonNull(requiredTrigger, "requiredTrigger");

        if (maximumCalendarDays < 0) {
            throw new IllegalArgumentException(
                    "maximumCalendarDays must not be negative"
            );
        }

        if (requiredTrigger == PaymentTerm.PaymentTrigger.UNKNOWN) {
            throw new IllegalArgumentException(
                    "A policy cannot require an unknown trigger"
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