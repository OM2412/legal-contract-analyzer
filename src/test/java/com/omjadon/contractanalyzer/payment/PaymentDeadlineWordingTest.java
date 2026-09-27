package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentDeadlineWordingTest {

    private static final PaymentPolicy POLICY = new PaymentPolicy(
            "P-DEMO-30",
            "1.0",
            30,
            PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
    );

    @Test
    void noLaterThanAfterInvoiceReceiptMeetsPolicy() {
        String clause = "Client shall pay Provider the project fee "
                + "no later than 30 calendar days after receipt of the invoice.";
        SourceDocument document = new SourceDocument("agreement", "1", clause);

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        assertEquals(clause, term.evidence().quote());
        assertEquals(PaymentTerm.PaymentTrigger.INVOICE_RECEIPT, term.trigger());
        assertEquals(
                PaymentPolicyEvaluator.Status.WITHIN_POLICY,
                PaymentPolicyEvaluator.evaluate(document, term, POLICY)
                        .status()
        );
    }

    @Test
    void noLaterThanAfterInvoiceDateDoesNotMeetReceiptPolicy() {
        String clause = "Client shall pay Provider the project fee "
                + "no later than 30 calendar days after the invoice date.";
        SourceDocument document = new SourceDocument("agreement", "1", clause);

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        assertEquals(clause, term.evidence().quote());
        assertEquals(PaymentTerm.PaymentTrigger.INVOICE_DATE, term.trigger());
        assertEquals(
                PaymentPolicyEvaluator.Status.DEVIATION,
                PaymentPolicyEvaluator.evaluate(document, term, POLICY)
                        .status()
        );
    }

    @Test
    void ofReceiptOfInvoiceIsRecognizedAsInvoiceReceipt() {
        String clause = "Client shall pay Provider the project fee "
                + "within 30 calendar days of receipt of the invoice.";
        SourceDocument document = new SourceDocument("agreement", "1", clause);

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        assertEquals(clause, term.evidence().quote());
        assertEquals(30, term.days());
        assertEquals(PaymentTerm.PaymentTrigger.INVOICE_RECEIPT, term.trigger());
        assertEquals(
                PaymentPolicyEvaluator.Status.WITHIN_POLICY,
                PaymentPolicyEvaluator.evaluate(document, term, POLICY)
                        .status()
        );
    }
}