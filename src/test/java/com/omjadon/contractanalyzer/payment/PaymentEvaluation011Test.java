package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentEvaluation011Test {

    @Test
    void milestonePaymentsWithoutDayCountsRequireManualReview() {
        String agreementText = "Client shall pay Provider 25% of the "
                + "project fee on signing this Agreement and the "
                + "remaining 75% on final acceptance.";

        SourceDocument agreement = new SourceDocument(
                "agreement", "1", agreementText
        );
        SourceDocument sow = new SourceDocument(
                "sow", "1",
                "Client shall pay Provider the project fee within "
                        + "60 calendar days after receipt of the invoice."
        );
        PaymentPolicy policy = new PaymentPolicy(
                "P-DEMO-30",
                "1.0",
                30,
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
        );

        PaymentReviewService.Review review =
                PaymentReviewService.analyze(agreement, sow, policy);

        assertEquals(
                PaymentReviewService.ReviewStatus.REVIEW_REQUIRED,
                review.status()
        );
        assertTrue(review.agreementMatches().isEmpty());
        assertFalse(review.agreementUnrecognized().isEmpty());
        assertEquals(
                agreementText,
                review.agreementUnrecognized().get(0).quote()
        );
        assertTrue(review.agreementAssessment().isEmpty());
        assertTrue(review.textualCandidateAssessment().isEmpty());
    }
}