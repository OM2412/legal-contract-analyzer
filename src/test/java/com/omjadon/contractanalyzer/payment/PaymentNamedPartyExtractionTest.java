package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentNamedPartyExtractionTest {

    private static final String CLAUSE =
            "Customer shall pay Atlas Systems the monthly support "
                    + "charge within 24 calendar days after Customer "
                    + "receives Atlas Systems' invoice.";

    @Test
    void extractsNamedPayeeSupportChargeAndPayerInvoiceReceipt() {
        SourceDocument document = new SourceDocument(
                "agreement", "1", CLAUSE
        );

        var terms = PaymentTermExtractor.extractAll(document);

        assertEquals(1, terms.size());

        var term = terms.get(0);

        assertEquals(24, term.days());
        assertEquals(
                PaymentTerm.DayUnit.CALENDAR_DAYS,
                term.dayUnit()
        );
        assertEquals(
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT,
                term.trigger()
        );
        assertEquals("Customer", term.payer());
        assertEquals("Atlas Systems", term.payee());
        assertEquals("monthly support charge", term.scope());
        assertEquals(CLAUSE, term.evidence().quote());

        EvidenceValidator.validate(document, term.evidence());
    }

    @Test
    void negatedNamedPartyPaymentIsNotExtracted() {
        SourceDocument document = new SourceDocument(
                "negative", "1",
                CLAUSE.replace("shall pay", "shall not pay")
        );

        assertTrue(
                PaymentTermExtractor.extractAll(document).isEmpty()
        );
    }

    @Test
    void anotherPartysReceiptIsNotAssignedToThePayer() {
        SourceDocument document = new SourceDocument(
                "wrong-receiver", "1",
                CLAUSE.replace(
                        "after Customer receives",
                        "after Provider receives"
                )
        );

        assertTrue(
                PaymentTermExtractor.extractAll(document).isEmpty()
        );
    }

    @Test
    void explicitlyNonBindingExampleIsNotExtracted() {
        SourceDocument document = new SourceDocument(
                "example", "1",
                "This illustrative example is not binding: \""
                        + CLAUSE + "\""
        );

        assertTrue(
                PaymentTermExtractor.extractAll(document).isEmpty()
        );
    }
}