package com.omjadon.contractanalyzer.evidence;

import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;

import java.util.Objects;

public final class EvidenceValidator {

    private EvidenceValidator() {
        // Static methods only.
    }

    public static void validate(SourceDocument document, EvidenceSpan evidence) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(evidence, "evidence");

        if (!document.documentId().equals(evidence.documentId())) {
            throw new IllegalArgumentException("Evidence document ID does not match");
        }

        if (!document.version().equals(evidence.documentVersion())) {
            throw new IllegalArgumentException("Evidence document version does not match");
        }

        String text = document.text();
        int totalCodePoints = text.codePointCount(0, text.length());

        if (evidence.end() > totalCodePoints) {
            throw new IllegalArgumentException("Evidence range exceeds document text");
        }

        // Java String indexes use UTF-16. Evidence positions use Unicode code points.
        int startIndex = text.offsetByCodePoints(0, evidence.start());
        int endIndex = text.offsetByCodePoints(0, evidence.end());

        String actualText = text.substring(startIndex, endIndex);

        if (!actualText.equals(evidence.quote())) {
            throw new IllegalArgumentException(
                    "Evidence quote does not match the document at this position"
            );
        }
    }

    public static EvidenceSpan fromRange(
            SourceDocument document,
            int start,
            int end
    ) {
        Objects.requireNonNull(document, "document");

        int totalCodePoints = document.text().codePointCount(
                0, document.text().length()
        );

        if (start < 0 || end <= start || end > totalCodePoints) {
            throw new IllegalArgumentException("Invalid evidence range");
        }

        int startIndex = document.text().offsetByCodePoints(0, start);
        int endIndex = document.text().offsetByCodePoints(0, end);

        EvidenceSpan evidence = new EvidenceSpan(
                document.documentId(),
                document.version(),
                start,
                end,
                document.text().substring(startIndex, endIndex)
        );

        validate(document, evidence);
        return evidence;
    }
}