package com.omjadon.contractanalyzer.model;

import java.util.Objects;
import java.util.Set;

/**
 * An immutable snapshot of extracted document text.
 *
 * OCR page numbers are one-based. Empty means no page used OCR.
 */
public record SourceDocument(
        String documentId,
        String version,
        String text,
        Set<Integer> ocrPages
) {

    public SourceDocument(String documentId, String version, String text) {
        this(documentId, version, text, Set.of());
    }

    public SourceDocument {
        requireNonBlank(documentId, "documentId");
        requireNonBlank(version, "version");
        requireNonBlank(text, "text");

        Objects.requireNonNull(ocrPages, "ocrPages");
        ocrPages = Set.copyOf(ocrPages);

        for (int page : ocrPages) {
            if (page < 1) {
                throw new IllegalArgumentException(
                        "OCR page numbers must start at 1"
                );
            }
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