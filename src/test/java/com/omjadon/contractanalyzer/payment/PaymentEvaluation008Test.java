package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.document.TextDocumentLoader;
import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentEvaluation008Test {

    private static final String PAYMENT_SENTENCE =
            "Client shall pay Provider the project fee within "
                    + "30 calendar days after receipt of the invoice.";

    @Test
    void illustrativeQuotedExampleDoesNotBecomePaymentTerm()
            throws IOException {
        SourceDocument agreement = TextDocumentLoader.load(
                Path.of("testdata/evaluation/EVAL-008-agreement.txt"),
                "eval-008-agreement",
                "1"
        );
        SourceDocument sow = TextDocumentLoader.load(
                Path.of("testdata/sow.txt"),
                "eval-008-sow",
                "1"
        );

        assertTrue(
                PaymentTermExtractor.extractAll(agreement).isEmpty()
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
        assertTrue(review.agreementAssessment().isEmpty());
        assertTrue(review.comparison().isEmpty());
    }

    @Test
    void quotedOperativeClauseWithoutDisclaimerCanStillBeExtracted() {
        SourceDocument document = new SourceDocument(
                "quoted-operative",
                "1",
                "The operative payment obligation is: \""
                        + PAYMENT_SENTENCE + "\""
        );

        List<PaymentTerm> terms =
                PaymentTermExtractor.extractAll(document);

        assertEquals(1, terms.size());
        assertEquals(PAYMENT_SENTENCE, terms.get(0).evidence().quote());
        assertEquals(30, terms.get(0).days());
        assertEquals(
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT,
                terms.get(0).trigger()
        );
    }
}