package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;
import com.omjadon.contractanalyzer.sample.SampleDocuments;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentReviewTest {

    @Test
    void detectsDifferenceBetweenSampleDocuments() {
        SourceDocument agreement = SampleDocuments.agreement();
        SourceDocument sow = SampleDocuments.statementOfWork();

        PaymentTerm first = PaymentTermExtractor.extract(agreement)
                .orElseThrow();
        PaymentTerm second = PaymentTermExtractor.extract(sow)
                .orElseThrow();

        PaymentTermComparator.Comparison result =
                PaymentTermComparator.compare(
                        agreement, first, sow, second
                );

        assertEquals(30, first.days());
        assertEquals(60, second.days());
        assertEquals(
                PaymentTermComparator.Status.POTENTIAL_DIFFERENCE,
                result.status()
        );
    }

    @Test
    void rejectsQuoteThatDoesNotMatchItsSource() {
        SourceDocument document = new SourceDocument(
                "agreement-test",
                "1",
                "Payment is due in 30 days."
        );

        EvidenceSpan incorrect = new EvidenceSpan(
                "agreement-test",
                "1",
                18,
                20,
                "90"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> EvidenceValidator.validate(document, incorrect)
        );
    }

    @Test
    void handlesUnicodeCodePointPositions() {
        SourceDocument document = new SourceDocument(
                "unicode-test",
                "1",
                "A😀 payment"
        );

        EvidenceSpan evidence = EvidenceValidator.fromRange(
                document,
                1,
                2
        );

        assertEquals("😀", evidence.quote());
        EvidenceValidator.validate(document, evidence);
    }

    @Test
    void abstainsWhenBusinessDaysMeetCalendarDayPolicy() {
        SourceDocument document = new SourceDocument(
                "business-days-test",
                "1",
                "Client shall pay Provider the project fee within "
                        + "30 business days after receipt of the invoice."
        );

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        PaymentPolicy policy = new PaymentPolicy(
                "P-DEMO-30",
                "1.0",
                30,
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
        );

        PaymentPolicyEvaluator.Assessment result =
                PaymentPolicyEvaluator.evaluate(
                        document,
                        term,
                        policy
                );

        assertEquals(PaymentTerm.DayUnit.BUSINESS_DAYS, term.dayUnit());
        assertEquals(
                PaymentPolicyEvaluator.Status.UNABLE_TO_ASSESS,
                result.status()
        );
        assertTrue(result.explanation().contains("calendar days"));
    }
}