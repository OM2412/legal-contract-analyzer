package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentDueAndPayableTest {

    @Test
    void recognizesExplicitProjectFeePayerAndPayee() {
        String clause = "The project fee is due and payable by Client "
                + "to Provider within 30 calendar days after receipt "
                + "of the invoice.";
        SourceDocument document = new SourceDocument(
                "agreement", "1", clause
        );

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        assertEquals(30, term.days());
        assertEquals(PaymentTerm.DayUnit.CALENDAR_DAYS, term.dayUnit());
        assertEquals(PaymentTerm.PaymentTrigger.INVOICE_RECEIPT, term.trigger());
        assertEquals("project_fee", term.scope());
        assertEquals("Client", term.payer());
        assertEquals("Provider", term.payee());
        assertEquals(clause, term.evidence().quote());
    }

    @Test
    void reversedPayerAndPayeeAreNotAssignedClientProviderRoles() {
        SourceDocument document = new SourceDocument(
                "agreement",
                "1",
                "The project fee is due and payable by Provider "
                        + "to Client within 30 calendar days after receipt "
                        + "of the invoice."
        );

        assertTrue(PaymentTermExtractor.extract(document).isEmpty());
    }
}