package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentLinkedInvoiceTest {

    @Test
    void linkedSentencesProvideThePaymentTermAndFullEvidence() {
        String text = "Provider will issue an invoice for the project fee. "
                + "Client must pay that invoice within 45 calendar days "
                + "of receiving it.";
        SourceDocument document = new SourceDocument("agreement", "1", text);

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        assertEquals(45, term.days());
        assertEquals(PaymentTerm.DayUnit.CALENDAR_DAYS, term.dayUnit());
        assertEquals(PaymentTerm.PaymentTrigger.INVOICE_RECEIPT, term.trigger());
        assertEquals(text, term.evidence().quote());

        PaymentPolicy policy = new PaymentPolicy(
                "P-DEMO-30",
                "1.0",
                30,
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
        );

        assertEquals(
                PaymentPolicyEvaluator.Status.DEVIATION,
                PaymentPolicyEvaluator.evaluate(document, term, policy)
                        .status()
        );
    }

    @Test
    void secondSentenceAloneDoesNotEstablishProjectFeeScope() {
        SourceDocument document = new SourceDocument(
                "agreement",
                "1",
                "Client must pay that invoice within 45 calendar days "
                        + "of receiving it."
        );

        assertTrue(PaymentTermExtractor.extract(document).isEmpty());
    }
}