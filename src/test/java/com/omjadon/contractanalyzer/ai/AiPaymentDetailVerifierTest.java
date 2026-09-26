package com.omjadon.contractanalyzer.ai;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiPaymentDetailVerifierTest {

    @Test
    void acceptsDaysAndUnitPresentInVerifiedQuote() {
        EvidenceSpan evidence = evidence(
                "Client must pay within 45 calendar days "
                        + "after receipt of the invoice."
        );

        var candidate = AiPaymentDetailVerifier.verify(
                """
                {"days":45,"dayUnit":"CALENDAR_DAYS",
                 "trigger":"INVOICE_RECEIPT"}
                """,
                evidence
        );

        assertTrue(candidate.isPresent());
        assertEquals(45, candidate.orElseThrow().days());
        assertEquals(
                "CALENDAR_DAYS",
                candidate.orElseThrow().dayUnit()
        );
        assertTrue(
                candidate.orElseThrow().invoiceReceiptConfirmed()
        );
    }

    @Test
    void rejectsDaysInventedByModel() {
        EvidenceSpan evidence = evidence(
                "Client must pay within 45 calendar days "
                        + "after receipt of the invoice."
        );

        var candidate = AiPaymentDetailVerifier.verify(
                """
                {"days":60,"dayUnit":"CALENDAR_DAYS",
                 "trigger":"INVOICE_RECEIPT"}
                """,
                evidence
        );

        assertTrue(candidate.isEmpty());
    }

    @Test
    void rejectsQuoteWithTwoDifferentPeriods() {
        EvidenceSpan evidence = evidence(
                "The first invoice is due in 30 calendar days. "
                        + "The second invoice is due in 45 calendar days."
        );

        var candidate = AiPaymentDetailVerifier.verify(
                """
                {"days":45,"dayUnit":"CALENDAR_DAYS",
                 "trigger":"INVOICE_RECEIPT"}
                """,
                evidence
        );

        assertTrue(candidate.isEmpty());
    }

    @Test
    void leavesAmbiguousInvoiceReferenceUnconfirmed() {
        EvidenceSpan evidence = evidence(
                "Provider will issue an invoice. Client must pay "
                        + "within 45 calendar days of receiving it."
        );

        var candidate = AiPaymentDetailVerifier.verify(
                """
                {"days":45,"dayUnit":"CALENDAR_DAYS",
                 "trigger":"INVOICE_RECEIPT"}
                """,
                evidence
        );

        assertTrue(candidate.isPresent());
        assertFalse(
                candidate.orElseThrow().invoiceReceiptConfirmed()
        );
    }

    private static EvidenceSpan evidence(String quote) {
        SourceDocument document = new SourceDocument(
                "test-document",
                "1",
                quote
        );

        return EvidenceValidator.fromRange(
                document,
                0,
                quote.codePointCount(0, quote.length())
        );
    }
}