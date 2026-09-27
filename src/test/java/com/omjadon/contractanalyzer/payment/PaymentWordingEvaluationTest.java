package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentWordingEvaluationTest {

    private record Example(
            String filename,
            int days,
            PaymentTerm.DayUnit unit,
            PaymentTerm.PaymentTrigger trigger
    ) {
    }

    private static final List<Example> SINGLE_CLAUSE_EXAMPLES = List.of(
            new Example("agreement.txt", 30,
                    PaymentTerm.DayUnit.CALENDAR_DAYS,
                    PaymentTerm.PaymentTrigger.INVOICE_RECEIPT),
            new Example("variant-agreement.txt", 45,
                    PaymentTerm.DayUnit.CALENDAR_DAYS,
                    PaymentTerm.PaymentTrigger.INVOICE_RECEIPT),
            new Example("final-acceptance-agreement.txt", 30,
                    PaymentTerm.DayUnit.CALENDAR_DAYS,
                    PaymentTerm.PaymentTrigger.FINAL_ACCEPTANCE),
            new Example("business-days-agreement.txt", 30,
                    PaymentTerm.DayUnit.BUSINESS_DAYS,
                    PaymentTerm.PaymentTrigger.INVOICE_RECEIPT),
            new Example("unspecified-days-agreement.txt", 30,
                    PaymentTerm.DayUnit.UNKNOWN,
                    PaymentTerm.PaymentTrigger.INVOICE_RECEIPT),
            new Example("invoice-date-agreement.txt", 30,
                    PaymentTerm.DayUnit.CALENDAR_DAYS,
                    PaymentTerm.PaymentTrigger.INVOICE_DATE),
            new Example("no-later-than-agreement.txt", 30,
                    PaymentTerm.DayUnit.CALENDAR_DAYS,
                    PaymentTerm.PaymentTrigger.INVOICE_RECEIPT),
            new Example("of-receipt-agreement.txt", 30,
                    PaymentTerm.DayUnit.CALENDAR_DAYS,
                    PaymentTerm.PaymentTrigger.INVOICE_RECEIPT),
            new Example("alternate-agreement.txt", 45,
                    PaymentTerm.DayUnit.CALENDAR_DAYS,
                    PaymentTerm.PaymentTrigger.INVOICE_RECEIPT),
            new Example("due-and-payable-agreement.txt", 30,
                    PaymentTerm.DayUnit.CALENDAR_DAYS,
                    PaymentTerm.PaymentTrigger.INVOICE_RECEIPT)
    );

    @Test
    void singleClauseFixturesMatchTheirExpectedFacts() {
        assertAll(
                "single-clause fixtures",
                SINGLE_CLAUSE_EXAMPLES.stream()
                        .map(example -> (Executable) () -> {
                            SourceDocument document = load(example.filename());
                            List<PaymentTerm> terms =
                                    PaymentTermExtractor.extractAll(document);

                            assertEquals(1, terms.size(), example.filename());

                            PaymentTerm term = terms.get(0);
                            assertEquals(
                                    example.days(), term.days(),
                                    example.filename()
                            );
                            assertEquals(
                                    example.unit(), term.dayUnit(),
                                    example.filename()
                            );
                            assertEquals(
                                    example.trigger(), term.trigger(),
                                    example.filename()
                            );
                            EvidenceValidator.validate(
                                    document, term.evidence()
                            );
                        })
        );
    }

    @Test
    void ambiguousFixturesRetainBothClauses() {
        assertAll(
                "ambiguous fixtures",
                List.of(
                        "conflicting-payment-agreement.txt",
                        "mixed-payment-agreement.txt"
                ).stream().map(filename -> (Executable) () -> {
                    SourceDocument document = load(filename);

                    assertEquals(
                            2,
                            PaymentTermExtractor.extractAll(document).size(),
                            filename
                    );
                })
        );
    }

    @Test
    void negationAndReversedPartiesProduceNoSupportedTerm()
            throws Exception {
        SourceDocument document = load("negative-payment-agreement.txt");

        assertTrue(
                PaymentTermExtractor.extractAll(document).isEmpty(),
                "Negation or reversed payer/payee must not be treated "
                        + "as a supported Client-to-Provider obligation"
        );
    }

    private static SourceDocument load(String filename) throws Exception {
        Path path = Path.of("testdata", filename);
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return new SourceDocument(filename, "evaluation-1", text);
    }
}