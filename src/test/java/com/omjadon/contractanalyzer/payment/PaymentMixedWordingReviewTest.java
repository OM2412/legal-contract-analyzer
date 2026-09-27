package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentMixedWordingReviewTest {

    @Test
    void twoDifferentPaymentWordingsRequireReview() {
        String first = "The project fee is due and payable by Client "
                + "to Provider within 30 calendar days after receipt "
                + "of the invoice.";
        String second = "Client shall pay Provider the project fee within "
                + "45 calendar days after receipt of the invoice.";

        SourceDocument agreement = new SourceDocument(
                "agreement", "1", first + "\n" + second
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
                PaymentReviewService.analyze(agreement, sow, policy);

        assertEquals(
                PaymentReviewService.ReviewStatus.REVIEW_REQUIRED,
                review.status()
        );
        assertEquals(
                List.of(first, second),
                review.agreementMatches().stream()
                        .map(term -> term.evidence().quote())
                        .toList()
        );
        assertTrue(review.agreementTerm().isEmpty());
        assertTrue(review.agreementAssessment().isEmpty());
        assertTrue(review.comparison().isEmpty());
        assertTrue(review.textualCandidateAssessment().isEmpty());
    }
}