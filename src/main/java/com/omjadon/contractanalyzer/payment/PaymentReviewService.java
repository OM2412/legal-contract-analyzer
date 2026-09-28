package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;

import java.util.List;
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
            Optional<PaymentPolicyEvaluator.Assessment> textualCandidateAssessment,
            List<PaymentTerm> agreementMatches,
            List<PaymentTerm> sowMatches,
            List<EvidenceSpan> agreementUnrecognized,
            List<EvidenceSpan> sowUnrecognized
    ) {
        public Review {
            agreementMatches = List.copyOf(agreementMatches);
            sowMatches = List.copyOf(sowMatches);
            agreementUnrecognized = List.copyOf(agreementUnrecognized);
            sowUnrecognized = List.copyOf(sowUnrecognized);
        }
    }

    public static Review analyze(
            SourceDocument agreement,
            SourceDocument sow,
            PaymentPolicy policy
    ) {
        Objects.requireNonNull(agreement, "agreement");
        Objects.requireNonNull(sow, "sow");
        Objects.requireNonNull(policy, "policy");

        List<PaymentTerm> agreementMatches =
                PaymentTermExtractor.extractAll(agreement);
        List<PaymentTerm> sowMatches =
                PaymentTermExtractor.extractAll(sow);

        List<EvidenceSpan> agreementUnrecognized =
                PaymentClauseCoverageScanner.findUnrecognized(
                        agreement, agreementMatches
                );
        List<EvidenceSpan> sowUnrecognized =
                PaymentClauseCoverageScanner.findUnrecognized(
                        sow, sowMatches
                );

        Optional<PaymentTerm> agreementTerm =
                uniqueTerm(agreementMatches);
        Optional<PaymentTerm> sowTerm =
                uniqueTerm(sowMatches);

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

        if (agreementMatches.size() > 1
                || sowMatches.size() > 1
                || !agreementUnrecognized.isEmpty()
                || !sowUnrecognized.isEmpty()) {
            return new Review(
                    ReviewStatus.REVIEW_REQUIRED,
                    agreementTerm,
                    sowTerm,
                    Optional.empty(),
                    precedence,
                    agreementAssessment,
                    sowAssessment,
                    Optional.empty(),
                    agreementMatches,
                    sowMatches,
                    agreementUnrecognized,
                    sowUnrecognized
            );
        }

        if (agreementTerm.isEmpty() || sowTerm.isEmpty()) {
            return new Review(
                    ReviewStatus.INCOMPLETE,
                    agreementTerm,
                    sowTerm,
                    Optional.empty(),
                    precedence,
                    agreementAssessment,
                    sowAssessment,
                    Optional.empty(),
                    agreementMatches,
                    sowMatches,
                    agreementUnrecognized,
                    sowUnrecognized
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
            // Precedence identifies a textual candidate only.
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
                candidate,
                agreementMatches,
                sowMatches,
                agreementUnrecognized,
                sowUnrecognized
        );
    }

    private static Optional<PaymentTerm> uniqueTerm(
            List<PaymentTerm> matches
    ) {
        return matches.size() == 1
                ? Optional.of(matches.get(0))
                : Optional.empty();
    }
}