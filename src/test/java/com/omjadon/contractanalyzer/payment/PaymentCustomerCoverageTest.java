package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentCustomerCoverageTest {

    @Test
    void flagsUnparsedCustomerVendorDeadline() throws Exception {
        String text = Files.readString(
                Path.of(
                        "testdata", "evaluation",
                        "EVAL-002-partial.txt"
                ),
                StandardCharsets.UTF_8
        );

        SourceDocument agreement = new SourceDocument(
                "agreement", "1", text
        );
        SourceDocument sow = new SourceDocument(
                "sow",
                "1",
                "Client shall pay Provider the project fee within "
                        + "30 calendar days after receipt of the invoice."
        );
        PaymentPolicy policy = new PaymentPolicy(
                "P-DEMO-30",
                "1.0",
                30,
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
        );

        PaymentReviewService.Review review =
                PaymentReviewService.analyze(
                        agreement, sow, policy
                );

        assertEquals(
                PaymentReviewService.ReviewStatus.REVIEW_REQUIRED,
                review.status()
        );
        assertEquals(1, review.agreementMatches().size());
        assertEquals(
                "implementation_fee",
                review.agreementMatches().get(0).scope()
        );
        assertEquals(20, review.agreementMatches().get(0).days());
        assertEquals(1, review.agreementUnrecognized().size());
        assertTrue(review.comparison().isEmpty());

        EvidenceSpan flagged = review.agreementUnrecognized().get(0);
        assertEquals(
                "Customer shall pay Vendor the implementation fee "
                        + "within forty-five (45) calendar days "
                        + "after receiving a valid invoice.",
                flagged.quote()
        );
        EvidenceValidator.validate(agreement, flagged);
    }
}