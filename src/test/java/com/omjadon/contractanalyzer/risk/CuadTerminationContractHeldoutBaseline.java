package com.omjadon.contractanalyzer.risk;

import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.json.JsonParserFactory;

/**
 * Frozen generic termination scanner on CUAD's released test split.
 * This is a candidate retrieval audit, not legal classification accuracy.
 */
public final class CuadTerminationContractHeldoutBaseline {

    private static final String LABEL =
            "\"Termination For Convenience\"";

    private static final Path TEST = Path.of(
            "data", "external", "cuad", "test.json"
    );

    private record Span(int start, int end) {
        boolean overlaps(Span other) {
            return start < other.end && other.start < end;
        }
    }

    private static final class ContractResult {
        int evaluatedParagraphs;
        boolean goldPositive;
        boolean scannerPositive;
        boolean evidenceOverlap;
    }

    private CuadTerminationContractHeldoutBaseline() {
    }

    public static void main(String[] args) throws Exception {
        if (!Files.isRegularFile(TEST)) {
            throw new IllegalStateException(
                    "CUAD test file missing: " + TEST
            );
        }

        Map<String, Object> root = JsonParserFactory.getJsonParser()
                .parseMap(
                        Files.readString(TEST, StandardCharsets.UTF_8)
                );

        Map<String, ContractResult> contracts =
                new LinkedHashMap<>();

        int questionInstances = 0;
        int answerSpans = 0;
        int evaluatedParagraphs = 0;

        for (Object entry : list(root.get("data"), "data")) {
            Map<?, ?> contract = map(entry, "contract");
            String title = string(contract.get("title"), "title");

            for (Object paragraphObject : list(
                    contract.get("paragraphs"), "paragraphs"
            )) {
                Map<?, ?> paragraph =
                        map(paragraphObject, "paragraph");

                String context = string(
                        paragraph.get("context"), "context"
                );

                List<Span> gold = new ArrayList<>();
                int targetQuestions = 0;

                for (Object questionObject : list(
                        paragraph.get("qas"), "qas"
                )) {
                    Map<?, ?> question =
                            map(questionObject, "question");

                    String wording = string(
                            question.get("question"),
                            "question text"
                    );

                    if (!wording.contains(LABEL)) {
                        continue;
                    }

                    targetQuestions++;
                    questionInstances++;

                    for (Object answerObject : list(
                            question.get("answers"), "answers"
                    )) {
                        Map<?, ?> answer =
                                map(answerObject, "answer");

                        String quote = string(
                                answer.get("text"),
                                "answer text"
                        );

                        int start = number(
                                answer.get("answer_start"),
                                "answer_start"
                        );

                        int end = start + quote.length();

                        if (start < 0
                                || end > context.length()
                                || !context.substring(start, end)
                                        .equals(quote)) {
                            throw new IllegalStateException(
                                    "Gold offset mismatch in "
                                            + title
                            );
                        }

                        gold.add(new Span(start, end));
                        answerSpans++;
                    }
                }

                if (targetQuestions == 0) {
                    continue;
                }

                evaluatedParagraphs++;

                ContractResult result =
                        contracts.computeIfAbsent(
                                title,
                                ignored -> new ContractResult()
                        );

                result.evaluatedParagraphs++;

                SourceDocument document = new SourceDocument(
                        "cuad-test-" + evaluatedParagraphs,
                        "test",
                        context
                );

                List<Span> predicted = new ArrayList<>();

                for (RiskFinding finding :
                        TerminationSignalFinder.find(
                                document,
                                "agreement"
                        )) {
                    for (EvidenceSpan evidence :
                            finding.evidence()) {
                        int start =
                                context.offsetByCodePoints(
                                        0,
                                        evidence.start()
                                );

                        int end =
                                context.offsetByCodePoints(
                                        0,
                                        evidence.end()
                                );

                        if (!context.substring(start, end)
                                .equals(evidence.quote())) {
                            throw new IllegalStateException(
                                    "Scanner evidence mismatch in "
                                            + title
                            );
                        }

                        predicted.add(new Span(start, end));
                    }
                }

                result.goldPositive |= !gold.isEmpty();
                result.scannerPositive |= !predicted.isEmpty();

                result.evidenceOverlap |=
                        gold.stream().anyMatch(
                                goldSpan ->
                                        predicted.stream().anyMatch(
                                                goldSpan::overlaps
                                        )
                        );
            }
        }

        if (questionInstances == 0 || contracts.isEmpty()) {
            throw new IllegalStateException(
                    "No target CUAD questions found; verify the "
                            + "test schema and exact label before "
                            + "reporting results"
            );
        }

        int bothPositive = 0;
        int scannerOnly = 0;
        int goldOnly = 0;
        int bothNegative = 0;
        int aligned = 0;

        for (ContractResult result : contracts.values()) {
            if (result.goldPositive
                    && result.scannerPositive) {
                bothPositive++;

                if (result.evidenceOverlap) {
                    aligned++;
                }
            } else if (result.scannerPositive) {
                scannerOnly++;
            } else if (result.goldPositive) {
                goldOnly++;
            } else {
                bothNegative++;
            }
        }

        double precision = ratio(
                bothPositive,
                bothPositive + scannerOnly
        );

        double recall = ratio(
                bothPositive,
                bothPositive + goldOnly
        );

        double f1 = precision + recall == 0
                ? 0
                : 2 * precision * recall
                        / (precision + recall);

        System.out.println(
                "CUAD TERMINATION HELD-OUT CONTRACT AUDIT"
        );
        System.out.println("Source: test.json");
        System.out.println(
                "Unit: contract title with a target question"
        );
        System.out.println(
                "Evaluated contracts: " + contracts.size()
        );
        System.out.println(
                "Evaluated paragraphs: " + evaluatedParagraphs
        );
        System.out.println(
                "Target question instances: " + questionInstances
        );
        System.out.println(
                "Gold answer spans: " + answerSpans
        );
        System.out.println(
                "Gold-positive contracts: "
                        + (bothPositive + goldOnly)
        );
        System.out.println(
                "Scanner-positive contracts: "
                        + (bothPositive + scannerOnly)
        );
        System.out.println(
                "Both positive: " + bothPositive
        );
        System.out.println(
                "Scanner only: " + scannerOnly
        );
        System.out.println(
                "Gold only: " + goldOnly
        );
        System.out.println(
                "Both negative: " + bothNegative
        );
        System.out.println(
                "Both positive with evidence overlap: "
                        + aligned
        );
        System.out.println(
                "Both positive without evidence overlap: "
                        + (bothPositive - aligned)
        );

        System.out.printf(
                Locale.ROOT,
                "Candidate flag precision: %.3f%n"
                        + "Candidate flag recall: %.3f%n"
                        + "Candidate flag F1: %.3f%n",
                precision,
                recall,
                f1
        );

        System.out.println(
                "Interpretation: generic termination cue against "
                        + "CUAD's narrower Termination For Convenience "
                        + "label. These held-out candidate overlap "
                        + "figures are not legal classification "
                        + "accuracy. Evidence overlap does not prove "
                        + "clause classification."
        );
    }

    private static double ratio(
            int numerator,
            int denominator
    ) {
        return denominator == 0
                ? 0
                : (double) numerator / denominator;
    }

    private static Map<?, ?> map(
            Object value,
            String field
    ) {
        if (!(value instanceof Map<?, ?> result)) {
            throw new IllegalArgumentException(
                    "Expected object: " + field
            );
        }

        return result;
    }

    private static List<?> list(
            Object value,
            String field
    ) {
        if (!(value instanceof List<?> result)) {
            throw new IllegalArgumentException(
                    "Expected array: " + field
            );
        }

        return result;
    }

    private static String string(
            Object value,
            String field
    ) {
        if (!(value instanceof String result)) {
            throw new IllegalArgumentException(
                    "Expected string: " + field
            );
        }

        return result;
    }

    private static int number(
            Object value,
            String field
    ) {
        if (!(value instanceof Number result)) {
            throw new IllegalArgumentException(
                    "Expected number: " + field
            );
        }

        return result.intValue();
    }
}