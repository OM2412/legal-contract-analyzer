package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.SourceDocument;

import java.util.Objects;
import java.util.Optional;

public final class PaymentReviewService {

    private PaymentReviewService() {
    }

    public enum ReviewStatus {
        INCOMPLETE,
        TERMS_MATCH,
        SEPARATE_OBLIGATIONS,
        REVIEW_REQUIRED,
        SOW_TEXT_CANDIDATE,
        AGREEMENT_TEXT_CANDIDATE
    }

    public record Review(
            ReviewStatus status,
            Optional<PaymentTerm> agreementTerm,
            Optional<PaymentTerm> sowTerm,
            Optional<PaymentTermComparator.Comparison> comparison,
            PaymentPrecedenceDetector.Detection precedence,
            Optional<PaymentPolicyEvaluator.Assessment> agreementAssessment,
            Optional<PaymentPolicyEvaluator.Assessment> sowAssessment,
            Optional<PaymentPolicyEvaluator.Assessment> textualCandidateAssessment
    ) {
    }

    public static Review analyze(
            SourceDocument agreement,
            SourceDocument sow,
            PaymentPolicy policy
    ) {
        Objects.requireNonNull(agreement, "agreement");
        Objects.requireNonNull(sow, "sow");
        Objects.requireNonNull(policy, "policy");

        Optional<PaymentTerm> agreementTerm =
                PaymentTermExtractor.extract(agreement);

        Optional<PaymentTerm> sowTerm =
                PaymentTermExtractor.extract(sow);

        PaymentPrecedenceDetector.Detection precedence =
                PaymentPrecedenceDetector.detect(agreement, sow);

        Optional<PaymentPolicyEvaluator.Assessment> agreementAssessment =
                agreementTerm.map(term ->
                        PaymentPolicyEvaluator.evaluate(
                                agreement, term, policy
                        )
                );

        Optional<PaymentPolicyEvaluator.Assessment> sowAssessment =
                sowTerm.map(term ->
                        PaymentPolicyEvaluator.evaluate(
                                sow, term, policy
                        )
                );

        if (agreementTerm.isEmpty() || sowTerm.isEmpty()) {
            return new Review(
                    ReviewStatus.INCOMPLETE,
                    agreementTerm,
                    sowTerm,
                    Optional.empty(),
                    precedence,
                    agreementAssessment,
                    sowAssessment,
                    Optional.empty()
            );
        }

        PaymentTermComparator.Comparison comparison =
                PaymentTermComparator.compare(
                        agreement,
                        agreementTerm.orElseThrow(),
                        sow,
                        sowTerm.orElseThrow()
                );

        ReviewStatus status;
        Optional<PaymentPolicyEvaluator.Assessment> candidate =
                Optional.empty();

        if (comparison.status()
                == PaymentTermComparator.Status.CONSISTENT) {
            status = ReviewStatus.TERMS_MATCH;
        } else if (comparison.status()
                == PaymentTermComparator.Status.DIFFERENT_OBLIGATIONS) {
            status = ReviewStatus.SEPARATE_OBLIGATIONS;
        } else if (comparison.status()
                == PaymentTermComparator.Status.UNABLE_TO_COMPARE) {
            status = ReviewStatus.REVIEW_REQUIRED;
        } else {
            // A difference exists. Precedence may identify a textual
            // candidate; it does not determine legal enforceability.
            switch (precedence.status()) {
                case SOW_TEXT_PRIORITY -> {
                    status = ReviewStatus.SOW_TEXT_CANDIDATE;
                    candidate = sowAssessment;
                }
                case AGREEMENT_TEXT_PRIORITY -> {
                    status = ReviewStatus.AGREEMENT_TEXT_CANDIDATE;
                    candidate = agreementAssessment;
                }
                case MULTIPLE_RULES, NOT_LOCATED ->
                        status = ReviewStatus.REVIEW_REQUIRED;
                default -> throw new IllegalStateException(
                        "Unrecognized precedence status"
                );
            }
        }

        return new Review(
                status,
                agreementTerm,
                sowTerm,
                Optional.of(comparison),
                precedence,
                agreementAssessment,
                sowAssessment,
                candidate
        );
    }
}