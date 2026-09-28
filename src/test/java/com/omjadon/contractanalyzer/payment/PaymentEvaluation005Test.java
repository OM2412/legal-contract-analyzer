package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.document.TextDocumentLoader;
import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentEvaluation005Test {

    private static final Path AGREEMENT_PATH = Path.of(
            "testdata/evaluation/EVAL-005-agreement.txt"
    );

    @Test
    void extractsBothInstallmentsWithDistinctTriggersAndExactQuotes()
            throws IOException {
        SourceDocument agreement = TextDocumentLoader.load(
                AGREEMENT_PATH, "eval-005", "1"
        );

        List<PaymentTerm> terms =
                PaymentTermExtractor.extractAll(agreement);

        assertEquals(2, terms.size());

        PaymentTerm first = terms.get(0);
        assertEquals("Customer", first.payer());
        assertEquals("Vendor", first.payee());
        assertEquals("implementation_fee_40_percent", first.scope());
        assertEquals(10, first.days());
        assertEquals(
                PaymentTerm.DayUnit.CALENDAR_DAYS,
                first.dayUnit()
        );
        assertEquals(
                PaymentTerm.PaymentTrigger.OTHER,
                first.trigger()
        );
        assertEquals(
                "Customer shall pay Vendor 40% of the implementation fee "
                        + "within 10 calendar days after signing this Agreement.",
                first.evidence().quote()
        );

        PaymentTerm second = terms.get(1);
        assertEquals("Customer", second.payer());
        assertEquals("Vendor", second.payee());
        assertEquals("implementation_fee_60_percent", second.scope());
        assertEquals(15, second.days());
        assertEquals(
                PaymentTerm.DayUnit.CALENDAR_DAYS,
                second.dayUnit()
        );
        assertEquals(
                PaymentTerm.PaymentTrigger.FINAL_ACCEPTANCE,
                second.trigger()
        );
        assertEquals(
                "Customer shall pay Vendor the remaining 60% of the "
                        + "implementation fee within 15 calendar days "
                        + "after final acceptance of the deliverables.",
                second.evidence().quote()
        );

        EvidenceValidator.validate(agreement, first.evidence());
        EvidenceValidator.validate(agreement, second.evidence());
        assertTrue(first.evidence().end() <= second.evidence().start());
    }

    @Test
    void multipleInstallmentsRequireReviewInsteadOfOneSelectedTerm()
            throws IOException {
        SourceDocument agreement = TextDocumentLoader.load(
                AGREEMENT_PATH, "eval-005-agreement", "1"
        );
        SourceDocument sow = TextDocumentLoader.load(
                Path.of("testdata/sow.txt"), "eval-005-sow", "1"
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
        assertEquals(2, review.agreementMatches().size());
        assertTrue(review.agreementTerm().isEmpty());
        assertTrue(review.agreementAssessment().isEmpty());
        assertTrue(review.comparison().isEmpty());
        assertTrue(review.textualCandidateAssessment().isEmpty());
    }
}