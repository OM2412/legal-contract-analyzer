package com.omjadon.contractanalyzer.risk;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;
import com.omjadon.contractanalyzer.payment.PaymentPolicyEvaluator;
import com.omjadon.contractanalyzer.payment.PaymentReviewService;
import com.omjadon.contractanalyzer.payment.PaymentTerm;
import com.omjadon.contractanalyzer.payment.PaymentTermComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PaymentRiskFinder {

    private PaymentRiskFinder() {
    }

    public static List<RiskFinding> find(
            SourceDocument agreement,
            SourceDocument sow,
            PaymentReviewService.Review review
    ) {
        Objects.requireNonNull(agreement, "agreement");
        Objects.requireNonNull(sow, "sow");
        Objects.requireNonNull(review, "review");

        List<RiskFinding> findings = new ArrayList<>();

        addUnrecognized(
                findings, agreement, "agreement",
                review.agreementUnrecognized()
        );
        addUnrecognized(
                findings, sow, "sow",
                review.sowUnrecognized()
        );

        addMultipleClauses(
                findings, agreement, "agreement",
                review.agreementMatches()
        );
        addMultipleClauses(
                findings, sow, "sow",
                review.sowMatches()
        );

        addPolicyDeviation(
                findings, agreement, "agreement",
                review.agreementTerm(),
                review.agreementAssessment()
        );
        addPolicyDeviation(
                findings, sow, "sow",
                review.sowTerm(),
                review.sowAssessment()
        );

        if (review.comparison().isPresent()
                && review.comparison().orElseThrow().status()
                == PaymentTermComparator.Status.POTENTIAL_DIFFERENCE
                && review.agreementTerm().isPresent()
                && review.sowTerm().isPresent()) {

            EvidenceSpan agreementEvidence =
                    review.agreementTerm().orElseThrow().evidence();
            EvidenceSpan sowEvidence =
                    review.sowTerm().orElseThrow().evidence();

            EvidenceValidator.validate(agreement, agreementEvidence);
            EvidenceValidator.validate(sow, sowEvidence);

            findings.add(new RiskFinding(
                    "payment.document-difference",
                    RiskFinding.Category.PAYMENT,
                    RiskFinding.Signal.DOCUMENT_DIFFERENCE,
                    RiskFinding.Priority.REVIEW_ONLY,
                    "Agreement and SOW payment terms differ",
                    review.comparison().orElseThrow().explanation()
                            + " Review both clauses and their context.",
                    "PAYMENT_COMPARISON_V1",
                    List.of(agreementEvidence, sowEvidence)
            ));
        }

        return List.copyOf(findings);
    }

    private static void addUnrecognized(
            List<RiskFinding> findings,
            SourceDocument document,
            String documentRole,
            List<EvidenceSpan> passages
    ) {
        for (int index = 0; index < passages.size(); index++) {
            EvidenceSpan passage = passages.get(index);
            EvidenceValidator.validate(document, passage);

            findings.add(new RiskFinding(
                    "payment." + documentRole + ".coverage." + index,
                    RiskFinding.Category.PAYMENT,
                    RiskFinding.Signal.COVERAGE_GAP,
                    RiskFinding.Priority.REVIEW_ONLY,
                    "Payment wording needs manual review",
                    "This passage may describe a payment obligation, "
                            + "but its terms were not fully parsed.",
                    "PAYMENT_COVERAGE_V1",
                    List.of(passage)
            ));
        }
    }

    private static void addMultipleClauses(
            List<RiskFinding> findings,
            SourceDocument document,
            String documentRole,
            List<PaymentTerm> matches
    ) {
        if (matches.size() < 2) {
            return;
        }

        List<EvidenceSpan> evidence = matches.stream()
                .map(PaymentTerm::evidence)
                .toList();

        evidence.forEach(span ->
                EvidenceValidator.validate(document, span)
        );

        findings.add(new RiskFinding(
                "payment." + documentRole + ".multiple-clauses",
                RiskFinding.Category.PAYMENT,
                RiskFinding.Signal.AMBIGUOUS_WORDING,
                RiskFinding.Priority.REVIEW_ONLY,
                "Multiple payment clauses need review",
                "The document contains multiple recognized payment "
                        + "clauses. Review each obligation separately.",
                "PAYMENT_MULTIPLE_CLAUSES_V1",
                evidence
        ));
    }

    private static void addPolicyDeviation(
            List<RiskFinding> findings,
            SourceDocument document,
            String documentRole,
            Optional<PaymentTerm> term,
            Optional<PaymentPolicyEvaluator.Assessment> assessment
    ) {
        if (term.isEmpty()
                || assessment.isEmpty()
                || assessment.orElseThrow().status()
                != PaymentPolicyEvaluator.Status.DEVIATION) {
            return;
        }

        EvidenceSpan evidence = term.orElseThrow().evidence();
        EvidenceValidator.validate(document, evidence);

        findings.add(new RiskFinding(
                "payment." + documentRole + ".policy-deviation",
                RiskFinding.Category.PAYMENT,
                RiskFinding.Signal.POLICY_DEVIATION,
                RiskFinding.Priority.REVIEW_ONLY,
                "Payment term differs from the demo policy",
                assessment.orElseThrow().explanation(),
                "PAYMENT_POLICY_V1",
                List.of(evidence)
        ));
    }
}