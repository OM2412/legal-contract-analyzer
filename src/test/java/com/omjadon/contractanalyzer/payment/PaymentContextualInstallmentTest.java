package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentContextualInstallmentTest {

    private static final String FIRST =
            "Customer must pay Atlas Systems 35 percent of the "
                    + "migration fee within 12 business days "
                    + "after the invoice date.";

    private static final String SECOND =
            "Customer must pay Atlas Systems the remaining 65 percent "
                    + "within 8 calendar days after Customer gives "
                    + "final written acceptance of the migration deliverable.";

    @Test
    void adjacentRemainderHasFieldsAndBothSentencesAsEvidence() {
        SourceDocument document = document(FIRST + " " + SECOND);
        var terms = PaymentTermExtractor.extractAll(document);

        assertEquals(2, terms.size());
        assertEquals(FIRST, terms.get(0).evidence().quote());

        var remaining = terms.get(1);

        assertEquals(8, remaining.days());
        assertEquals(
                PaymentTerm.DayUnit.CALENDAR_DAYS,
                remaining.dayUnit()
        );
        assertEquals(
                PaymentTerm.PaymentTrigger.FINAL_ACCEPTANCE,
                remaining.trigger()
        );
        assertEquals("Customer", remaining.payer());
        assertEquals("Atlas Systems", remaining.payee());
        assertEquals(
                "remaining 65 percent of the migration fee",
                remaining.scope()
        );
        assertEquals(
                FIRST + " " + SECOND,
                remaining.evidence().quote()
        );

        terms.forEach(term ->
                EvidenceValidator.validate(document, term.evidence())
        );
    }

    @Test
    void remainderWithoutAntecedentStaysUnrecognized() {
        SourceDocument document = document(SECOND);
        var terms = PaymentTermExtractor.extractAll(document);

        assertTrue(terms.isEmpty());

        assertEquals(
                1,
                PaymentClauseCoverageScanner.findUnrecognized(
                        document, terms
                ).size()
        );
    }

    @Test
    void mismatchedPartiesReceiverOrDeliverableDoNotLink() {
        for (String second : List.of(
                SECOND.replaceFirst("Customer", "Client"),
                SECOND.replace("Atlas Systems", "Nova Labs"),
                SECOND.replace(
                        "after Customer gives",
                        "after Client gives"
                ),
                SECOND.replace(
                        "migration deliverable",
                        "support deliverable"
                )
        )) {
            assertOnlyFirst(FIRST + " " + second);
        }
    }

    @Test
    void invalidPercentageTotalDoesNotLink() {
        assertOnlyFirst(
                FIRST + " "
                        + SECOND.replace("65 percent", "60 percent")
        );
    }

    @Test
    void interveningTextParagraphOrPageBoundaryDoesNotLink() {
        for (String gap : List.of(
                " An unrelated fee applies. ",
                "\n\n",
                "\r\n\r\n",
                "\f"
        )) {
            assertOnlyFirst(FIRST + gap + SECOND);
        }
    }

    @Test
    void negatedRemainingPaymentDoesNotLink() {
        assertOnlyFirst(
                FIRST + " "
                        + SECOND.replace("must pay", "must not pay")
        );
    }

    @Test
    void quotedNonBindingPairDoesNotCreateEitherTerm() {
        for (String quotation : List.of(
                "\"" + FIRST + " " + SECOND + "\"",
                "\u201C" + FIRST + " " + SECOND + "\u201D"
        )) {
            SourceDocument document = document(
                    "This illustrative example is not binding: "
                            + quotation
            );

            assertTrue(
                    PaymentTermExtractor.extractAll(document).isEmpty()
            );
        }
    }

    @Test
    void unicodePrefixAndSingleLineBreakPreserveSourceRange() {
        String text = "\uD83D\uDCC4 " + FIRST + "\r\n" + SECOND;
        SourceDocument document = document(text);
        var terms = PaymentTermExtractor.extractAll(document);

        assertEquals(2, terms.size());

        var evidence = terms.get(1).evidence();

        assertEquals(2, evidence.start());
        assertEquals(
                text.codePointCount(0, text.length()),
                evidence.end()
        );
        assertEquals(
                FIRST + "\r\n" + SECOND,
                evidence.quote()
        );

        EvidenceValidator.validate(document, evidence);
    }

    private static void assertOnlyFirst(String text) {
        var terms = PaymentTermExtractor.extractAll(document(text));

        assertEquals(1, terms.size(), text);
        assertEquals(FIRST, terms.get(0).evidence().quote());
    }

    private static SourceDocument document(String text) {
        return new SourceDocument("sow", "regression", text);
    }
}