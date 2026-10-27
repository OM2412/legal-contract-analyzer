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

    private enum CueKind {
        CAP(
                "Possible liability limit wording",
                "Check the amount, scope, exceptions, and "
                        + "cross-references before interpreting "
                        + "this limit.",
                "LIABILITY_CAP_CUE_V1"
        ),
        EXCEPTION(
                "Possible liability-limit exception",
                "Check which obligation is excepted and whether "
                        + "other limits still apply. This cue does "
                        + "not establish uncapped liability.",
                "LIABILITY_EXCEPTION_CUE_V1"
        ),
        EXCLUDED_LOSS(
                "Possible excluded-loss wording",
                "Check which losses are excluded, the parties "
                        + "covered, and any exceptions.",
                "LIABILITY_EXCLUDED_LOSS_CUE_V1"
        );

        private final String title;
        private final String explanation;
        private final String ruleId;

        CueKind(
                String title,
                String explanation,
                String ruleId
        ) {
            this.title = title;
            this.explanation = explanation;
            this.ruleId = ruleId;
        }
    }

    private record Cue(Pattern pattern, CueKind kind) {
    }

    private record Range(
            int start,
            int end,
            CueKind kind
    ) {
    }

    private static final List<Cue> CUES = List.of(
            new Cue(
                    Pattern.compile(
                            "\\b(?:aggregate\\s+|total\\s+)?"
                                    + "liability\\b"
                                    + "[^.;\\f]{0,220}?"
                                    + "\\b(?:not\\s+exceed"
                                    + "|limited\\s+to"
                                    + "|capped\\s+at)\\b",
                            Pattern.CASE_INSENSITIVE
                    ),
                    CueKind.CAP
            ),
            new Cue(
                    Pattern.compile(
                            "\\b(?:shall|will|does)\\s+not\\s+"
                                    + "(?:limit|exclude)\\s+"
                                    + "(?:the\\s+)?liability\\b",
                            Pattern.CASE_INSENSITIVE
                    ),
                    CueKind.EXCEPTION
            ),
            new Cue(
                    Pattern.compile(
                            "\\b(?:shall|will)\\s+not\\s+"
                                    + "be\\s+liable\\b",
                            Pattern.CASE_INSENSITIVE
                    ),
                    CueKind.EXCLUDED_LOSS
            ),
            new Cue(
                    Pattern.compile(
                            "\\bneither\\s+party\\s+"
                                    + "(?:shall|will)\\s+"
                                    + "be\\s+liable\\b",
                            Pattern.CASE_INSENSITIVE
                    ),
                    CueKind.EXCLUDED_LOSS
            ),
            new Cue(
                    Pattern.compile(
                            "\\b(?:is|are)\\s+not\\s+liable\\b",
                            Pattern.CASE_INSENSITIVE
                    ),
                    CueKind.EXCLUDED_LOSS
            ),
            new Cue(
                    Pattern.compile(
                            "\\bin\\s+no\\s+event\\b"
                                    + "[^.;\\f]{0,180}?"
                                    + "\\b(?:liable|liability)\\b",
                            Pattern.CASE_INSENSITIVE
                    ),
                    CueKind.EXCLUDED_LOSS
            ),
            new Cue(
                    Pattern.compile(
                            "\\bwaives?\\s+any\\s+right\\s+"
                                    + "to\\s+recover\\b",
                            Pattern.CASE_INSENSITIVE
                    ),
                    CueKind.EXCLUDED_LOSS
            )
    );

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

        for (Cue cue : CUES) {
            Matcher matcher = cue.pattern().matcher(text);

            while (matcher.find()) {
                int start = sentenceStart(
                        text, matcher.start()
                );
                int end = sentenceEnd(
                        text, matcher.start(), matcher.end()
                );

                Range candidate = new Range(
                        start, end, cue.kind()
                );

                if (!ranges.contains(candidate)) {
                    ranges.add(candidate);
                }
            }
        }

        ranges.sort(
                Comparator.comparingInt(Range::start)
                        .thenComparingInt(Range::end)
                        .thenComparing(Range::kind)
        );

        List<RiskFinding> findings = new ArrayList<>();

        for (Range range : ranges) {
            int start = text.codePointCount(
                    0, range.start()
            );
            int end = text.codePointCount(
                    0, range.end()
            );

            EvidenceSpan evidence =
                    EvidenceValidator.fromRange(
                            document, start, end
                    );

            CueKind kind = range.kind();

            findings.add(new RiskFinding(
                    "liability." + documentRole + "."
                            + findings.size(),
                    RiskFinding.Category.LIABILITY,
                    RiskFinding.Signal.CLAUSE_FOR_REVIEW,
                    RiskFinding.Priority.REVIEW_ONLY,
                    kind.title,
                    kind.explanation,
                    kind.ruleId,
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
                && Character.isWhitespace(
                        text.charAt(index)
                )) {
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