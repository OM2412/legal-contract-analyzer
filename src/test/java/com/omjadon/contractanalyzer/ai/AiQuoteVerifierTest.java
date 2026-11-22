package com.omjadon.contractanalyzer.ai;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiQuoteVerifierTest {

    private static SourceDocument document(String text) {
        return new SourceDocument("agreement-upload", "1", text);
    }

    @Test
    void returnsVerbatimQuoteWithCodePointOffsets() {
        String quote = "Payment is due in 30 calendar days.";
        String content = "🧾 Agreement: " + quote;

        var results = AiQuoteVerifier.locateExactQuotes(
                document(content),
                "{\"quotes\":[\"Payment is due in 30 calendar days.\"]}"
        );

        assertEquals(1, results.size());
        assertEquals(quote, results.getFirst().quote());
        assertEquals("🧾 Agreement: ".codePointCount(0, "🧾 Agreement: ".length()),
                results.getFirst().start());
    }

    @Test
    void rejectsFabricatedQuote() {
        assertThrows(IllegalArgumentException.class, () ->
                AiQuoteVerifier.locateExactQuotes(
                        document("Payment due in 30 days."),
                        "{\"quotes\":[\"Payment due in 60 days.\"]}"
                )
        );
    }

    @Test
    void rejectsAmbiguousRepeatedQuote() {
        assertThrows(IllegalArgumentException.class, () ->
                AiQuoteVerifier.locateExactQuotes(
                        document("Payment due. Other terms. Payment due."),
                        "{\"quotes\":[\"Payment due.\"]}"
                )
        );
    }

    @Test
    void rejectsDuplicateQuoteEntries() {
        assertThrows(IllegalArgumentException.class, () ->
                AiQuoteVerifier.locateExactQuotes(
                        document("Payment due in 30 days."),
                        "{\"quotes\":[\"Payment due in 30 days.\",\"Payment due in 30 days.\"]}"
                )
        );
    }

    @Test
    void acceptsEmptySuggestionSet() {
        assertTrue(AiQuoteVerifier.locateExactQuotes(
                document("This document has no payment clause."),
                "{\"quotes\":[]}"
        ).isEmpty());
    }

    @Test
    void rejectsMalformedModelOutput() {
        assertThrows(IllegalArgumentException.class, () ->
                AiQuoteVerifier.locateExactQuotes(
                        document("Payment due in 30 days."),
                        "This is not JSON"
                )
        );
    }
}
