package com.omjadon.contractanalyzer.ai;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class AiQuoteVerifier {

    private static final int MAX_QUOTES = 5;
    private static final int MAX_QUOTE_CHARS = 1_200;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private AiQuoteVerifier() {
    }

    public static List<EvidenceSpan> locateExactQuotes(
            SourceDocument document,
            String modelJson
    ) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(modelJson, "modelJson");

        final JsonNode root;

        try {
            root = JSON.readTree(modelJson);
        } catch (JacksonException error) {
            throw new IllegalArgumentException(
                    "Ollama returned invalid JSON", error
            );
        }

        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException(
                    "Ollama response must be a JSON object"
            );
        }

        JsonNode quotes = root.get("quotes");

        if (quotes == null
                || !quotes.isArray()
                || quotes.size() > MAX_QUOTES) {
            throw new IllegalArgumentException(
                    "Ollama response must contain up to five quotes"
            );
        }

        List<EvidenceSpan> located = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int index = 0; index < quotes.size(); index++) {
            JsonNode entry = quotes.get(index);

            if (entry == null || !entry.isString()) {
                throw new IllegalArgumentException(
                        "Every AI quote must be a JSON string"
                );
            }

            String quote = entry.stringValue();

            if (quote.isBlank()
                    || quote.length() > MAX_QUOTE_CHARS
                    || !seen.add(quote)) {
                throw new IllegalArgumentException(
                        "AI quote is blank, repeated, or too long"
                );
            }

            int firstIndex = document.text().indexOf(quote);

            if (firstIndex < 0) {
                throw new IllegalArgumentException(
                        "AI quote was not found verbatim in the document"
                );
            }

            if (document.text().lastIndexOf(quote) != firstIndex) {
                throw new IllegalArgumentException(
                        "AI quote appears more than once; location is ambiguous"
                );
            }

            int endIndex = firstIndex + quote.length();
            int start = document.text().codePointCount(0, firstIndex);
            int end = document.text().codePointCount(0, endIndex);

            EvidenceSpan evidence = EvidenceValidator.fromRange(
                    document, start, end
            );

            located.add(evidence);
        }

        return List.copyOf(located);
    }
}