package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentInvoiceDateTest {

    private static final PaymentPolicy POLICY = new PaymentPolicy(
            "P-DEMO-30",
            "1.0",
            30,
            PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
    );

    @Test
    void invoiceDateIsRecognizedButDoesNotMeetInvoiceReceiptPolicy() {
        String clause = "Client shall pay Provider the project fee within "
                + "30 calendar days after the invoice date.";
        SourceDocument document = new SourceDocument(
                "agreement", "1", clause
        );

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        assertEquals(30, term.days());
        assertEquals(PaymentTerm.DayUnit.CALENDAR_DAYS, term.dayUnit());
        assertEquals(PaymentTerm.PaymentTrigger.INVOICE_DATE, term.trigger());
        assertEquals(clause, term.evidence().quote());
        assertEquals(
                PaymentPolicyEvaluator.Status.DEVIATION,
                PaymentPolicyEvaluator.evaluate(document, term, POLICY)
                        .status()
        );
    }

    @Test
    void invoiceReceiptRemainsASeparateTrigger() {
        String clause = "Client shall pay Provider the project fee within "
                + "30 calendar days after receipt of the invoice.";
        SourceDocument document = new SourceDocument(
                "agreement", "1", clause
        );

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        assertEquals(
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT,
                term.trigger()
        );
        assertEquals(
                PaymentPolicyEvaluator.Status.WITHIN_POLICY,
                PaymentPolicyEvaluator.evaluate(document, term, POLICY)
                        .status()
        );
    }

    @Test
    void equalDurationsWithDifferentTriggersAreNotConsistent() {
        SourceDocument agreement = new SourceDocument(
                "agreement",
                "1",
                "Client shall pay Provider the project fee within "
                        + "30 calendar days after the invoice date."
        );
        SourceDocument sow = new SourceDocument(
                "sow",
                "1",
                "Client shall pay Provider the project fee within "
                        + "30 calendar days after receipt of the invoice."
        );

        PaymentTermComparator.Comparison comparison =
                PaymentTermComparator.compare(
                        agreement,
                        PaymentTermExtractor.extract(agreement).orElseThrow(),
                        sow,
                        PaymentTermExtractor.extract(sow).orElseThrow()
                );

        assertEquals(
                PaymentTermComparator.Status.POTENTIAL_DIFFERENCE,
                comparison.status()
        );
    }
}