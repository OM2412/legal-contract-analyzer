package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.SourceDocument;
import com.omjadon.contractanalyzer.sample.SampleDocuments;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentReviewServiceTest {

    private static final PaymentPolicy DEMO_POLICY =
            new PaymentPolicy(
                    "P-DEMO-30",
                    "1.0",
                    30,
                    PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
            );

    @Test
    void usesExplicitSowPrecedenceAsTextualCandidate() {
        PaymentReviewService.Review review =
                PaymentReviewService.analyze(
                        SampleDocuments.agreement(),
                        SampleDocuments.statementOfWork(),
                        DEMO_POLICY
                );

        assertEquals(
                PaymentReviewService.ReviewStatus.SOW_TEXT_CANDIDATE,
                review.status()
        );

        assertEquals(
                PaymentPolicyEvaluator.Status.DEVIATION,
                review.textualCandidateAssessment()
                        .orElseThrow()
                        .status()
        );
    }

    @Test
    void reportsIncompleteWhenSowPaymentIsNotRecognized() {
        SourceDocument sow = new SourceDocument(
                "sow-incomplete",
                "1",
                "The payment schedule is provided in Attachment P."
        );

        PaymentReviewService.Review review =
                PaymentReviewService.analyze(
                        SampleDocuments.agreement(),
                        sow,
                        DEMO_POLICY
                );

        assertEquals(
                PaymentReviewService.ReviewStatus.INCOMPLETE,
                review.status()
        );
        assertTrue(review.sowTerm().isEmpty());
        assertTrue(review.comparison().isEmpty());
        assertTrue(review.textualCandidateAssessment().isEmpty());
    }

    @Test
    void opposingPrecedenceRulesRequireReview() {
        SourceDocument original = SampleDocuments.agreement();

        SourceDocument conflictingAgreement = new SourceDocument(
                original.documentId(),
                "2",
                original.text() + "\n"
                        + "If this Agreement and the SOW differ on payment "
                        + "for the project fee, the Agreement payment term prevails."
        );

        PaymentReviewService.Review review =
                PaymentReviewService.analyze(
                        conflictingAgreement,
                        SampleDocuments.statementOfWork(),
                        DEMO_POLICY
                );

        assertEquals(
                PaymentPrecedenceDetector.Status.MULTIPLE_RULES,
                review.precedence().status()
        );
        assertEquals(
                PaymentReviewService.ReviewStatus.REVIEW_REQUIRED,
                review.status()
        );
        assertTrue(review.textualCandidateAssessment().isEmpty());
    }
}