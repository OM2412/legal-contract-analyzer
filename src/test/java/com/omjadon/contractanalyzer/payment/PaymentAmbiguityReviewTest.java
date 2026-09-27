package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentAmbiguityReviewTest {

    @Test
    void keepsBothConflictingClausesWithoutChoosingOne() {
        String first =
                "Client shall pay Provider the project fee within "
                        + "30 calendar days after receipt of the invoice.";
        String second =
                "The Client must pay the Provider the project fee within "
                        + "45 calendar days after receiving the invoice.";

        SourceDocument agreement = new SourceDocument(
                "agreement",
                "1",
                first + "\n" + second
        );

        SourceDocument sow = new SourceDocument(
                "sow",
                "1",
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
                PaymentReviewService.analyze(
                        agreement,
                        sow,
                        policy
                );

        assertEquals(
                PaymentReviewService.ReviewStatus.REVIEW_REQUIRED,
                review.status()
        );
        assertEquals(2, review.agreementMatches().size());
        assertEquals(
                first,
                review.agreementMatches().get(0).evidence().quote()
        );
        assertEquals(
                second,
                review.agreementMatches().get(1).evidence().quote()
        );

        assertTrue(review.agreementTerm().isEmpty());
        assertTrue(review.agreementAssessment().isEmpty());
        assertTrue(review.comparison().isEmpty());
        assertTrue(review.textualCandidateAssessment().isEmpty());

        assertTrue(review.sowTerm().isPresent());
    }
}