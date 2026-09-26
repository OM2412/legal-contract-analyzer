package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;

import java.util.Objects;

public final class PaymentTermComparator {

    private PaymentTermComparator() {
    }

    public enum Status {
        CONSISTENT,
        POTENTIAL_DIFFERENCE,
        DIFFERENT_OBLIGATIONS,
        UNABLE_TO_COMPARE
    }

    public record Comparison(Status status, String explanation) {
    }

    public static Comparison compare(
            SourceDocument agreement,
            PaymentTerm agreementTerm,
            SourceDocument sow,
            PaymentTerm sowTerm
    ) {
        Objects.requireNonNull(agreement, "agreement");
        Objects.requireNonNull(agreementTerm, "agreementTerm");
        Objects.requireNonNull(sow, "sow");
        Objects.requireNonNull(sowTerm, "sowTerm");

        // Validate both source references before comparing extracted facts.
        EvidenceValidator.validate(agreement, agreementTerm.evidence());
        EvidenceValidator.validate(sow, sowTerm.evidence());

        if (!agreementTerm.scope().equalsIgnoreCase(sowTerm.scope())
                || !agreementTerm.payer().equalsIgnoreCase(sowTerm.payer())
                || !agreementTerm.payee().equalsIgnoreCase(sowTerm.payee())) {
            return new Comparison(
                    Status.DIFFERENT_OBLIGATIONS,
                    "These terms refer to different payment obligations."
            );
        }

        if (agreementTerm.dayUnit() == PaymentTerm.DayUnit.UNKNOWN
                || sowTerm.dayUnit() == PaymentTerm.DayUnit.UNKNOWN
                || agreementTerm.trigger() == PaymentTerm.PaymentTrigger.UNKNOWN
                || sowTerm.trigger() == PaymentTerm.PaymentTrigger.UNKNOWN) {
            return new Comparison(
                    Status.UNABLE_TO_COMPARE,
                    "The payment unit or trigger could not be identified."
            );
        }

        if (agreementTerm.days() != sowTerm.days()) {
            return new Comparison(
                    Status.POTENTIAL_DIFFERENCE,
                    "Payment periods differ: "
                            + agreementTerm.days() + " versus "
                            + sowTerm.days() + " days."
            );
        }

        if (agreementTerm.dayUnit() != sowTerm.dayUnit()) {
            return new Comparison(
                    Status.POTENTIAL_DIFFERENCE,
                    "One term uses calendar days and the other uses business days."
            );
        }

        if (agreementTerm.trigger() != sowTerm.trigger()) {
            return new Comparison(
                    Status.POTENTIAL_DIFFERENCE,
                    "The payment periods start after different events."
            );
        }

        return new Comparison(
                Status.CONSISTENT,
                "Both terms state the same payment timing for this obligation."
        );
    }
}