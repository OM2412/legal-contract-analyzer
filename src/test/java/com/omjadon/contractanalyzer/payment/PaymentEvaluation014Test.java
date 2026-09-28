package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentEvaluation014Test {

    @Test
    void paymentMethodDoesNotHideInvoiceDeadline() {
        String text = "Client shall pay Provider the project fee by "
                + "electronic bank transfer within 30 calendar days "
                + "after receipt of the invoice.";
        SourceDocument document = new SourceDocument(
                "agreement", "1", text
        );

        List<PaymentTerm> terms = PaymentTermExtractor.extractAll(document);

        assertEquals(1, terms.size());
        PaymentTerm term = terms.get(0);
        assertEquals(30, term.days());
        assertEquals(PaymentTerm.DayUnit.CALENDAR_DAYS, term.dayUnit());
        assertEquals(
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT,
                term.trigger()
        );
        assertEquals("Client", term.payer());
        assertEquals("Provider", term.payee());
        assertEquals("project_fee", term.scope());
        assertEquals(text, term.evidence().quote());
        assertTrue(
                PaymentClauseCoverageScanner
                        .findUnrecognized(document, terms)
                        .isEmpty()
        );
    }
}