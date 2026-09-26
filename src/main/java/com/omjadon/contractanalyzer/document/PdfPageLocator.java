package com.omjadon.contractanalyzer.document;

import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;

import java.util.Objects;

public final class PdfPageLocator {

    private PdfPageLocator() {
    }

    public record PageRange(int firstPage, int lastPage) {
        public PageRange {
            if (firstPage < 1 || lastPage < firstPage) {
                throw new IllegalArgumentException("Invalid PDF page range");
            }
        }
    }

    public static PageRange locate(
            SourceDocument document,
            EvidenceSpan evidence
    ) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(evidence, "evidence");

        if (!document.documentId().equals(evidence.documentId())
                || !document.version().equals(evidence.documentVersion())) {
            throw new IllegalArgumentException(
                    "Evidence belongs to a different document version"
            );
        }

        String text = document.text();
        int codePointLength = text.codePointCount(0, text.length());

        if (evidence.start() < 0
                || evidence.end() <= evidence.start()
                || evidence.end() > codePointLength) {
            throw new IllegalArgumentException("Invalid evidence offsets");
        }

        if (text.indexOf('\f') < 0) {
            throw new IllegalArgumentException(
                    "Document has no PDF page markers"
            );
        }

        int startIndex = text.offsetByCodePoints(0, evidence.start());
        int endIndex = text.offsetByCodePoints(0, evidence.end());

        if (!text.substring(startIndex, endIndex).equals(evidence.quote())) {
            throw new IllegalArgumentException(
                    "Evidence quote does not match extracted PDF text"
            );
        }

        int firstPage = 1 + pageMarkersBefore(text, startIndex);
        int lastPage = 1 + pageMarkersBefore(text, endIndex - 1);

        return new PageRange(firstPage, lastPage);
    }

    private static int pageMarkersBefore(String text, int endExclusive) {
        int count = 0;

        for (int index = 0; index < endExclusive; index++) {
            if (text.charAt(index) == '\f') {
                count++;
            }
        }

        return count;
    }
}