package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.omjadon.contractanalyzer.ai.AiPaymentDetailVerifier;
class PaymentTermWordingTest {

    private static final String NEW_WORDING =
            "The Client must pay the Provider the project fee "
                    + "within 45 calendar days after receiving the invoice.";

    private static final String ORIGINAL_WORDING =
            "Client shall pay Provider the project fee "
                    + "within 30 calendar days after receipt of the invoice.";

    @Test
    void recognizesMustPayAndReceivingTheInvoice() {
        SourceDocument document =
                new SourceDocument("variant", "1", NEW_WORDING);

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        assertEquals(45, term.days());
        assertEquals(
                PaymentTerm.DayUnit.CALENDAR_DAYS,
                term.dayUnit()
        );
        assertEquals(
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT,
                term.trigger()
        );
        assertEquals("Client", term.payer());
        assertEquals("Provider", term.payee());
        assertEquals(NEW_WORDING, term.evidence().quote());
    }

    @Test
    void stillRecognizesOriginalWording() {
        SourceDocument document =
                new SourceDocument("original", "1", ORIGINAL_WORDING);

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        assertEquals(30, term.days());
        assertEquals(
                PaymentTerm.DayUnit.CALENDAR_DAYS,
                term.dayUnit()
        );
        assertEquals(
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT,
                term.trigger()
        );
        assertEquals(ORIGINAL_WORDING, term.evidence().quote());
    }

    @Test
    void doesNotRecognizeNegatedPaymentInstruction() {
        String text =
                "The Client must not pay the Provider the project fee "
                        + "within 45 calendar days after receiving the invoice.";
        SourceDocument document =
                new SourceDocument("negated", "1", text);

        assertTrue(PaymentTermExtractor.extract(document).isEmpty());
    }

    @Test
    void rejectsMultipleMatchingPaymentClauses() {
        SourceDocument document = new SourceDocument(
                "multiple",
                "1",
                ORIGINAL_WORDING + "\n" + NEW_WORDING
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> PaymentTermExtractor.extract(document)
        );
    }
    @Test
    void keepsFinalAcceptanceSeparateFromInvoiceReceipt() {
        String text =
                "Client shall pay Provider the project fee within "
                        + "30 calendar days after final acceptance.";

        SourceDocument document =
                new SourceDocument("acceptance", "1", text);

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        assertEquals(30, term.days());
        assertEquals(
                PaymentTerm.DayUnit.CALENDAR_DAYS,
                term.dayUnit()
        );
        assertEquals(
                PaymentTerm.PaymentTrigger.FINAL_ACCEPTANCE,
                term.trigger()
        );
        assertEquals(text, term.evidence().quote());
    }
    @Test
    void businessDaysAreNotTreatedAsCalendarDays() {
        String text =
                "Client shall pay Provider the project fee within "
                        + "30 business days after receipt of the invoice.";

        SourceDocument document =
                new SourceDocument("business-days", "1", text);

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        assertEquals(30, term.days());
        assertEquals(
                PaymentTerm.DayUnit.BUSINESS_DAYS,
                term.dayUnit()
        );
        assertEquals(text, term.evidence().quote());

        PaymentPolicy policy = new PaymentPolicy(
                "P-DEMO-30",
                "1.0",
                30,
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
        );

        var assessment =
                PaymentPolicyEvaluator.evaluate(document, term, policy);

        assertEquals(
                PaymentPolicyEvaluator.Status.UNABLE_TO_ASSESS,
                assessment.status()
        );
    }
    @Test
    void doesNotGuessCalendarDaysWhenUnitIsMissing() {
        String text =
                "Client shall pay Provider the project fee within "
                        + "30 days after receipt of the invoice.";

        SourceDocument document =
                new SourceDocument("unspecified-days", "1", text);

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        assertEquals(30, term.days());
        assertEquals(PaymentTerm.DayUnit.UNKNOWN, term.dayUnit());
        assertEquals(text, term.evidence().quote());

        PaymentPolicy policy = new PaymentPolicy(
                "P-DEMO-30",
                "1.0",
                30,
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
        );

        var assessment =
                PaymentPolicyEvaluator.evaluate(document, term, policy);

        assertEquals(
                PaymentPolicyEvaluator.Status.UNABLE_TO_ASSESS,
                assessment.status()
        );

        var inventedCalendarUnit = AiPaymentDetailVerifier.verify(
                """
                {"days":30,"dayUnit":"CALENDAR_DAYS",
                 "trigger":"INVOICE_RECEIPT"}
                """,
                term.evidence()
        );

        assertTrue(inventedCalendarUnit.isEmpty());
    }}