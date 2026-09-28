package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentClauseCoverageScanner {

    private static final Pattern PAYMENT_START = Pattern.compile(
            "\\b(?:(?:Client|Customer)"
                    + "\\s+(?:shall|must)\\s+pay\\s+"
                    + "(?:the\\s+)?(?:Provider|Vendor)"
                    + "\\s+(?:\\d{1,3}%\\s+of\\s+)?the\\s+"
                    + "(?:project|implementation)\\s+fee"
                    + "|The\\s+project\\s+fee\\s+is\\s+due\\s+and"
                    + "\\s+payable\\s+by\\s+Client\\s+to\\s+Provider)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_CONTEXT_CHARS = 400;

    private PaymentClauseCoverageScanner() {
    }

    public static List<EvidenceSpan> findUnrecognized(
            SourceDocument document,
            List<PaymentTerm> recognizedTerms
    ) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(recognizedTerms, "recognizedTerms");

        String text = document.text();
        Matcher matcher = PAYMENT_START.matcher(text);
        List<EvidenceSpan> unrecognized = new ArrayList<>();

        while (matcher.find()) {
            int cueStart = text.codePointCount(0, matcher.start());
            int cueEnd = text.codePointCount(0, matcher.end());

            boolean covered = recognizedTerms.stream().anyMatch(term ->
                    term.evidence().start() <= cueStart
                            && term.evidence().end() >= cueEnd
            );

            if (covered) {
                continue;
            }

            int endIndex = matcher.end();

            while (endIndex < text.length()
                    && endIndex - matcher.end() < MAX_CONTEXT_CHARS
                    && text.charAt(endIndex) != '.'
                    && text.charAt(endIndex) != '\n'
                    && text.charAt(endIndex) != '\r') {
                endIndex++;
            }

            if (endIndex < text.length() && text.charAt(endIndex) == '.') {
                endIndex++;
            }

            int end = text.codePointCount(0, endIndex);

            unrecognized.add(
                    EvidenceValidator.fromRange(document, cueStart, end)
            );
        }

        return List.copyOf(unrecognized);
    }
}