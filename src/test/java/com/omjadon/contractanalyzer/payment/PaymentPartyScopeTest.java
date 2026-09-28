package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentPartyScopeTest {

    @Test
    void extractsCustomerVendorImplementationFee() throws Exception {
        String clause = Files.readString(
                Path.of(
                        "testdata", "evaluation",
                        "EVAL-001-agreement.txt"
                ),
                StandardCharsets.UTF_8
        ).trim();

        SourceDocument document = new SourceDocument(
                "evaluation-001", "1", clause
        );

        PaymentTerm term = PaymentTermExtractor.extract(document)
                .orElseThrow();

        assertEquals("Customer", term.payer());
        assertEquals("Vendor", term.payee());
        assertEquals("implementation_fee", term.scope());
        assertEquals(20, term.days());
        assertEquals(PaymentTerm.DayUnit.CALENDAR_DAYS, term.dayUnit());
        assertEquals(PaymentTerm.PaymentTrigger.INVOICE_RECEIPT, term.trigger());
        assertEquals(clause, term.evidence().quote());
    }

    @Test
    void differentPartiesAndFeeAreDifferentObligations()
            throws Exception {
        String implementationClause = Files.readString(
                Path.of(
                        "testdata", "evaluation",
                        "EVAL-001-agreement.txt"
                ),
                StandardCharsets.UTF_8
        ).trim();

        SourceDocument agreement = new SourceDocument(
                "agreement", "1", implementationClause
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
                PaymentTermComparator.Status.DIFFERENT_OBLIGATIONS,
                comparison.status()
        );
    }

    @Test
    void negatedCustomerPaymentIsNotExtracted() {
        SourceDocument document = new SourceDocument(
                "negated",
                "1",
                "Customer shall not pay Vendor the implementation fee "
                        + "within 20 calendar days after receiving "
                        + "a valid invoice."
        );

        assertTrue(PaymentTermExtractor.extractAll(document).isEmpty());
    }
}