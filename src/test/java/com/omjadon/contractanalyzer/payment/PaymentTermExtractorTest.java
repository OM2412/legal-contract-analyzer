package com.omjadon.contractanalyzer.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

class PaymentTermExtractorTest {

    @Test
    void recognizesFollowingReceiptOfAnInvoice() {
        String clause = "Client shall pay Provider the project fee within "
                + "45 business days following receipt of an invoice.";
        SourceDocument document = new SourceDocument("agreement", "v1", clause);

        PaymentTerm term = PaymentTermExtractor.extract(document).orElseThrow();

        assertEquals(45, term.days());
        assertEquals(PaymentTerm.DayUnit.BUSINESS_DAYS, term.dayUnit());
        assertEquals(clause, term.evidence().quote());
    }

    @Test
    void doesNotAssumeCalendarDaysWhenUnitIsMissing() {
        String clause = "Client shall pay Provider the project fee within "
                + "30 days after receipt of the invoice.";
        SourceDocument document = new SourceDocument("agreement", "v1", clause);

        PaymentTerm term = PaymentTermExtractor.extract(document).orElseThrow();

        assertEquals(30, term.days());
        assertEquals(PaymentTerm.DayUnit.UNKNOWN, term.dayUnit());
    }

    @Test
    void rejectsMultipleMatchingPaymentClauses() {
        String clause = "Client shall pay Provider the project fee within "
                + "30 calendar days after receipt of the invoice.";
        SourceDocument document = new SourceDocument(
                "agreement", "v1", clause + "\n" + clause
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> PaymentTermExtractor.extract(document)
        );

        assertTrue(error.getMessage().contains("Multiple matching"));
    }

    @Test
    void doesNotTreatPermissionToPayAsAnObligation() {
        SourceDocument document = new SourceDocument(
                "agreement",
                "v1",
                "Client may pay Provider the project fee within "
                        + "30 calendar days after receipt of the invoice."
        );

        assertTrue(PaymentTermExtractor.extract(document).isEmpty());
    }
}