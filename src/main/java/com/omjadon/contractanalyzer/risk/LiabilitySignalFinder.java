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

/** Finds narrow liability wording cues for human review. */
public final class LiabilitySignalFinder {
    private static final int MAX_CONTEXT_CHARS = 500;
    private static final int MAX_BACK_CONTEXT_CHARS = 160;

    private static final List<Pattern> CUES = List.of(
            // A possible ceiling; this does not identify its exceptions.
            Pattern.compile(
                    "\\b(?:aggregate\\s+|total\\s+)?liability\\b"
                            + "[^.;\\f]{0,220}?"
                            + "\\b(?:not\\s+exceed|limited\\s+to"
                            + "|capped\\s+at)\\b",
                    Pattern.CASE_INSENSITIVE
            ),

            // An exception to a limitation.
            Pattern.compile(
                    "\\b(?:shall|will|does)\\s+not\\s+"
                            + "(?:limit|exclude)\\s+(?:the\\s+)?"
                            + "liability\\b",
                    Pattern.CASE_INSENSITIVE
            ),

            // Exclusions of liability or recovery.
            Pattern.compile(
                    "\\b(?:shall|will)\\s+not\\s+be\\s+liable\\b",
                    Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                    "\\bneither\\s+party\\s+(?:shall|will)\\s+"
                            + "be\\s+liable\\b",
                    Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                    "\\b(?:is|are)\\s+not\\s+liable\\b",
                    Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                    "\\bin\\s+no\\s+event\\b[^.;\\f]{0,180}?"
                            + "\\b(?:liable|liability)\\b",
                    Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                    "\\bwaives?\\s+any\\s+right\\s+to\\s+recover\\b",
                    Pattern.CASE_INSENSITIVE
            )
    );

    private record Range(int start, int end) {
    }

    private LiabilitySignalFinder() {
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

        for (Pattern pattern : CUES) {
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                int start = sentenceStart(text, matcher.start());
                int end = sentenceEnd(
                        text, matcher.start(), matcher.end()
                );

                boolean duplicate = false;

                for (Range previous : ranges) {
                    if (previous.start() == start
                            && previous.end() == end) {
                        duplicate = true;
                        break;
                    }
                }

                if (!duplicate) {
                    ranges.add(new Range(start, end));
                }
            }
        }

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
                    "liability." + documentRole + "."
                            + findings.size(),
                    RiskFinding.Category.LIABILITY,
                    RiskFinding.Signal.CLAUSE_FOR_REVIEW,
                    RiskFinding.Priority.REVIEW_ONLY,
                    "Liability wording found",
                    "Check the full clause for caps, excluded losses, "
                            + "carve-outs, and whether any exception applies.",
                    "LIABILITY_WORDING_V1",
                    List.of(evidence)
            ));
        }

        return List.copyOf(findings);
    }

    private static int sentenceStart(
            String text,
            int cueStart
    ) {
        int lowerBound = Math.max(
                0, cueStart - MAX_BACK_CONTEXT_CHARS
        );
        int index = cueStart;

        while (index > lowerBound
                && text.charAt(index - 1) != '.'
                && text.charAt(index - 1) != ';'
                && text.charAt(index - 1) != '\f') {
            index--;
        }

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

    private static int sentenceEnd(
            String text,
            int cueStart,
            int cueEnd
    ) {
        int index = cueEnd;

        while (index < text.length()
                && index - cueStart < MAX_CONTEXT_CHARS
                && text.charAt(index) != '.'
                && text.charAt(index) != '\f') {
            index++;
        }

        if (index < text.length()
                && text.charAt(index) == '.') {
            index++;
        }

        while (index > cueEnd
                && Character.isWhitespace(
                        text.charAt(index - 1)
                )) {
            index--;
        }

        return index;
    }
}