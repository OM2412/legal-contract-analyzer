package com.omjadon.contractanalyzer.model;

/**
 * A quoted passage from a specific document version.
 *
 * Positions use zero-based Unicode code points.
 * Start is inclusive; end is exclusive.
 */
public record EvidenceSpan(
        String documentId,
        String documentVersion,
        int start,
        int end,
        String quote
) {

    public EvidenceSpan {
        requireNonBlank(documentId, "documentId");
        requireNonBlank(documentVersion, "documentVersion");
        requireNonBlank(quote, "quote");

        if (start < 0) {
            throw new IllegalArgumentException(
                    "start must not be negative"
            );
        }

        if (end <= start) {
            throw new IllegalArgumentException(
                    "end must be greater than start"
            );
        }

        int quoteLength = quote.codePointCount(0, quote.length());

        if (quoteLength != end - start) {
            throw new IllegalArgumentException(
                    "quote length must match the evidence range"
            );
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null or blank"
            );
        }
    }
}