package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.document.TextDocumentLoader;
import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentEvaluation004Test {

    private static final Path FIXTURE = Path.of(
            "testdata/evaluation/EVAL-004-agreement.txt"
    );

    @Test
    void extractsPassiveVoicePaymentWithExactSourceEvidence()
            throws IOException {
        SourceDocument document = TextDocumentLoader.load(
                FIXTURE, "eval-004", "1"
        );

        List<PaymentTerm> matches =
                PaymentTermExtractor.extractAll(document);

        assertEquals(1, matches.size());

        PaymentTerm term = matches.get(0);

        assertEquals("Customer", term.payer());
        assertEquals("Vendor", term.payee());
        assertEquals("implementation_fee", term.scope());
        assertEquals(20, term.days());
        assertEquals(
                PaymentTerm.DayUnit.CALENDAR_DAYS,
                term.dayUnit()
        );
        assertEquals(
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT,
                term.trigger()
        );

        String rawSource = Files.readString(
                FIXTURE, StandardCharsets.UTF_8
        ).strip();

        assertEquals(0, term.evidence().start());
        assertEquals(rawSource, term.evidence().quote());
        assertTrue(term.evidence().quote().contains(
                "Customer receives a valid invoice."
        ));

        EvidenceValidator.validate(document, term.evidence());
    }

    @Test
    void doesNotTreatAnotherPartysInvoiceReceiptAsPayerReceipt()
            throws IOException {
        SourceDocument original = TextDocumentLoader.load(
                FIXTURE, "eval-004-source", "1"
        );

        String altered = original.text().replace(
                "after Customer receives",
                "after Vendor receives"
        );

        SourceDocument document = new SourceDocument(
                "eval-004-negative", "1", altered
        );

        assertTrue(PaymentTermExtractor.extractAll(document).isEmpty());
    }
}