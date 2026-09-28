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

class PaymentPartialCoverageReviewTest {

    private static final String NORMAL_CLAUSE =
            "Client shall pay Provider the project fee within "
                    + "30 calendar days after receipt of the invoice.";

    private static final PaymentPolicy POLICY = new PaymentPolicy(
            "P-DEMO-30",
            "1.0",
            30,
            PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
    );

    @Test
    void unparsedAdditionalClauseRequiresReview() throws Exception {
        SourceDocument agreement = load(
                "partially-recognized-agreement.txt"
        );
        SourceDocument sow = new SourceDocument(
                "sow", "1", NORMAL_CLAUSE
        );

        PaymentReviewService.Review review =
                PaymentReviewService.analyze(agreement, sow, POLICY);

        assertEquals(
                PaymentReviewService.ReviewStatus.REVIEW_REQUIRED,
                review.status()
        );
        assertEquals(1, review.agreementMatches().size());
        assertEquals(1, review.agreementUnrecognized().size());
        assertTrue(review.comparison().isEmpty());
        assertTrue(review.textualCandidateAssessment().isEmpty());

        EvidenceSpan flagged = review.agreementUnrecognized().get(0);
        assertEquals(
                "Client shall pay Provider the project fee within "
                        + "ninety (90) calendar days after receipt "
                        + "of the invoice.",
                flagged.quote()
        );
        EvidenceValidator.validate(agreement, flagged);
    }

    @Test
    void fullyRecognizedDocumentsHaveNoCoverageWarning() {
        SourceDocument agreement = new SourceDocument(
                "agreement", "1", NORMAL_CLAUSE
        );
        SourceDocument sow = new SourceDocument(
                "sow", "1", NORMAL_CLAUSE
        );

        PaymentReviewService.Review review =
                PaymentReviewService.analyze(agreement, sow, POLICY);

        assertEquals(
                PaymentReviewService.ReviewStatus.TERMS_MATCH,
                review.status()
        );
        assertTrue(review.agreementUnrecognized().isEmpty());
        assertTrue(review.sowUnrecognized().isEmpty());
    }

    @Test
    void negationAndReversedPartiesDoNotProduceCoverageWarning()
            throws Exception {
        SourceDocument agreement = load(
                "negative-payment-agreement.txt"
        );
        SourceDocument sow = new SourceDocument(
                "sow", "1", NORMAL_CLAUSE
        );

        PaymentReviewService.Review review =
                PaymentReviewService.analyze(agreement, sow, POLICY);

        assertEquals(
                PaymentReviewService.ReviewStatus.INCOMPLETE,
                review.status()
        );
        assertTrue(review.agreementMatches().isEmpty());
        assertTrue(review.agreementUnrecognized().isEmpty());
    }

    private static SourceDocument load(String filename) throws Exception {
        String text = Files.readString(
                Path.of("testdata", filename),
                StandardCharsets.UTF_8
        );
        return new SourceDocument(filename, "1", text);
    }
}