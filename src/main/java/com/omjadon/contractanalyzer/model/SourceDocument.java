package com.omjadon.contractanalyzer.model;

/**
 * An immutable snapshot of a document's extracted text.
 *
 * Original text is preserved so evidence positions remain valid.
 */
public record SourceDocument(
        String documentId,
        String version,
        String text
) {

    public SourceDocument {
        requireNonBlank(documentId, "documentId");
        requireNonBlank(version, "version");
        requireNonBlank(text, "text");
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be null or blank"
            );
        }
    }
}