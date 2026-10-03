package com.omjadon.contractanalyzer.risk;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TerminationSignalFinder {
    private static final String DOCUMENT =
            "(?:Agreement|SOW|Statement\\s+of\\s+Work)";

    // Preserve the existing supported pattern and its evidence boundary.
    private static final Pattern EXPLICIT_TERMINATION = Pattern.compile(
            "\\b(?:(?:Either|Each)\\s+party"
                    + "|(?:the\\s+)?(?:Client|Customer|Provider|Vendor))"
                    + "\\s+may\\s+terminate"
                    + "\\s+(?:this|the)\\s+" + DOCUMENT + "\\b",
            Pattern.CASE_INSENSITIVE
    );

    // These additional forms indicate wording to review, not legal effect.
    private static final Pattern ACTOR_MAY_TERMINATE = Pattern.compile(
            "\\bmay\\s+terminate\\s+(?:this|the)\\s+"
                    + DOCUMENT + "\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PASSIVE_TERMINATION = Pattern.compile(
            "\\b(?:this|the)\\s+" + DOCUMENT
                    + "\\s+may\\s+(?:also\\s+)?be\\s+terminated\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TERMINATED_BY_EITHER_PARTY =
            Pattern.compile(
                    "\\b(?:this|the)\\s+" + DOCUMENT
                            + "\\b[^.;\\f]{0,200}?\\bterminated\\s+by"
                            + "\\s+either\\s+party\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern ENTITLED_TO_TERMINATE = Pattern.compile(
            "\\bis\\s+entitled\\s+to\\s+(?:unilaterally\\s+)?"
                    + "terminate\\s+(?:this|the)\\s+" + DOCUMENT
                    + "\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_CONTEXT_CHARS = 450;
    private static final int MAX_BACK_CONTEXT_CHARS = 180;

    private record Range(
            int cueStart,
            int cueEnd,
            int start,
            int end,
            String ruleId
    ) {
    }

    private TerminationSignalFinder() {
    }

    public static List<RiskFinding> find(
            SourceDocument document,
            String documentRole
    ) {
        Objects.requireNonNull(document, "document");

        if (!"agreement".equals(documentRole)
                && !"sow".equals(documentRole)) {
            throw new IllegalArgumentException(
                    "documentRole must be agreement or sow"
            );
        }

        String text = document.text();
        List<Range> ranges = new ArrayList<>();

        addMatches(
                text,
                EXPLICIT_TERMINATION,
                false,
                "TERMINATION_EXPLICIT_V1",
                ranges
        );
        addMatches(
                text,
                ACTOR_MAY_TERMINATE,
                true,
                "TERMINATION_CUE_V2",
                ranges
        );
        addMatches(
                text,
                PASSIVE_TERMINATION,
                true,
                "TERMINATION_CUE_V2",
                ranges
        );
        addMatches(
                text,
                TERMINATED_BY_EITHER_PARTY,
                true,
                "TERMINATION_CUE_V2",
                ranges
        );
        addMatches(
                text,
                ENTITLED_TO_TERMINATE,
                true,
                "TERMINATION_CUE_V2",
                ranges
        );

        ranges.sort(
                Comparator.comparingInt(Range::start)
                        .thenComparingInt(Range::end)
        );

        List<RiskFinding> findings = new ArrayList<>();

        for (Range range : ranges) {
            int start = text.codePointCount(0, range.start());
            int end = text.codePointCount(0, range.end());

            EvidenceSpan evidence = EvidenceValidator.fromRange(
                    document,
                    start,
                    end
            );

            findings.add(new RiskFinding(
                    "termination." + documentRole + "."
                            + findings.size(),
                    RiskFinding.Category.TERMINATION,
                    RiskFinding.Signal.CLAUSE_FOR_REVIEW,
                    RiskFinding.Priority.REVIEW_ONLY,
                    "Termination wording found",
                    "Read the full clause and surrounding text for "
                            + "notice requirements, grounds, exceptions, "
                            + "and which party may terminate.",
                    range.ruleId(),
                    List.of(evidence)
            ));
        }

        return List.copyOf(findings);
    }

    private static void addMatches(
            String text,
            Pattern pattern,
            boolean includeSentenceStart,
            String ruleId,
            List<Range> ranges
    ) {
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            boolean alreadyCovered = ranges.stream().anyMatch(range ->
                    range.cueStart() <= matcher.start()
                            && range.cueEnd() >= matcher.end()
            );

            if (alreadyCovered) {
                continue;
            }

            int startIndex = includeSentenceStart
                    ? sentenceStart(text, matcher.start())
                    : matcher.start();

            int endIndex = matcher.end();

            while (endIndex < text.length()
                    && endIndex - matcher.start() < MAX_CONTEXT_CHARS
                    && text.charAt(endIndex) != '.'
                    && text.charAt(endIndex) != '\f') {
                endIndex++;
            }

            if (endIndex < text.length()
                    && text.charAt(endIndex) == '.') {
                endIndex++;
            }

            while (endIndex > matcher.end()
                    && Character.isWhitespace(
                            text.charAt(endIndex - 1)
                    )) {
                endIndex--;
            }

            boolean duplicateEvidence = false;

            for (Range range : ranges) {
                if (range.start() == startIndex
                        && range.end() == endIndex) {
                    duplicateEvidence = true;
                    break;
                }
            }

            if (duplicateEvidence) {
                continue;
            }

            ranges.add(new Range(
                    matcher.start(),
                    matcher.end(),
                    startIndex,
                    endIndex,
                    ruleId
            ));
        }
    }

    private static int sentenceStart(
            String text,
            int cueStart
    ) {
        int lowerBound = Math.max(
                0,
                cueStart - MAX_BACK_CONTEXT_CHARS
        );
        int index = cueStart;

        while (index > lowerBound
                && text.charAt(index - 1) != '.'
                && text.charAt(index - 1) != ';'
                && text.charAt(index - 1) != '\f') {
            index--;
        }

        // Avoid beginning an evidence quote in the middle of a word.
        if (index == lowerBound
                && index > 0
                && !Character.isWhitespace(
                        text.charAt(index - 1)
                )) {
            while (index < cueStart
                    && !Character.isWhitespace(
                            text.charAt(index)
                    )) {
                index++;
            }
        }

        while (index < cueStart
                && Character.isWhitespace(text.charAt(index))) {
            index++;
        }

        return index;
    }
}