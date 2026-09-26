package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;

import java.util.Objects;

public final class PaymentPolicyEvaluator {

    private PaymentPolicyEvaluator() {
    }

    public enum Status {
        WITHIN_POLICY,
        DEVIATION,
        UNABLE_TO_ASSESS
    }

    public record Assessment(Status status, String explanation) {
    }

    public static Assessment evaluate(
            SourceDocument document,
            PaymentTerm term,
            PaymentPolicy policy
    ) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(term, "term");
        Objects.requireNonNull(policy, "policy");

        EvidenceValidator.validate(document, term.evidence());

        if (term.dayUnit() != PaymentTerm.DayUnit.CALENDAR_DAYS) {
            return new Assessment(
                    Status.UNABLE_TO_ASSESS,
                    "This policy uses calendar days. The extracted term "
                            + "cannot be compared without a supported conversion."
            );
        }

        if (term.trigger() == PaymentTerm.PaymentTrigger.UNKNOWN) {
            return new Assessment(
                    Status.UNABLE_TO_ASSESS,
                    "The payment trigger could not be identified."
            );
        }

        if (term.trigger() != policy.requiredTrigger()) {
            return new Assessment(
                    Status.DEVIATION,
                    "Payment starts after " + term.trigger()
                            + ", while policy " + policy.policyId()
                            + " requires " + policy.requiredTrigger() + "."
            );
        }

        if (term.days() > policy.maximumCalendarDays()) {
            return new Assessment(
                    Status.DEVIATION,
                    "Payment period is " + term.days()
                            + " calendar days; policy "
                            + policy.policyId() + " version "
                            + policy.version() + " allows at most "
                            + policy.maximumCalendarDays() + "."
            );
        }

        return new Assessment(
                Status.WITHIN_POLICY,
                "This payment term meets policy "
                        + policy.policyId() + " version "
                        + policy.version() + "."
        );
    }
}