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
 * Training-only candidate retrieval audit for two CUAD labels.
 *
 * The product scanner finds generic liability wording and does not
 * classify a clause as capped or uncapped.
 */
public final class CuadLiabilityContractTrainBaseline {

    private static final Path TRAIN = Path.of(
            "data", "external", "cuad",
            "train_separate_questions.json"
    );

    private static final List<String> LABELS = List.of(
            "Cap On Liability",
            "Uncapped Liability"
    );

    private record Span(int start, int end) {
        boolean overlaps(Span other) {
            return start < other.end && other.start < end;
        }
    }

    private static final class ContractResult {
        boolean goldPositive;
        boolean scannerPositive;
        boolean evidenceOverlap;
    }

    private static final class LabelResult {
        int paragraphs;
        int questions;
        int answerSpans;

        final Map<String, ContractResult> contracts =
                new LinkedHashMap<>();
    }

    private CuadLiabilityContractTrainBaseline() {
    }

    public static void main(String[] args) throws Exception {
        if (!Files.isRegularFile(TRAIN)) {
            throw new IllegalStateException(
                    "CUAD training file missing: " + TRAIN
            );
        }

        Map<String, Object> root = JsonParserFactory.getJsonParser()
                .parseMap(Files.readString(TRAIN, StandardCharsets.UTF_8));

        Map<String, LabelResult> results = new LinkedHashMap<>();
        for (String label : LABELS) {
            results.put(label, new LabelResult());
        }

        int contextNumber = 0;

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

                Map<String, List<Span>> goldByLabel =
                        new LinkedHashMap<>();
                Map<String, Integer> questionsByLabel =
                        new LinkedHashMap<>();

                for (String label : LABELS) {
                    goldByLabel.put(label, new ArrayList<>());
                    questionsByLabel.put(label, 0);
                }

                for (Object questionObject : list(
                        paragraph.get("qas"), "qas"
                )) {
                    Map<?, ?> question =
                            map(questionObject, "question");
                    String wording = string(
                            question.get("question"), "question"
                    );

                    for (String label : LABELS) {
                        if (!wording.contains(
                                "\"" + label + "\""
                        )) {
                            continue;
                        }

                        questionsByLabel.put(
                                label,
                                questionsByLabel.get(label) + 1
                        );

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
                                                + " for " + label
                                );
                            }

                            goldByLabel.get(label).add(
                                    new Span(start, end)
                            );
                        }
                    }
                }

                boolean relevant = LABELS.stream()
                        .anyMatch(label ->
                                questionsByLabel.get(label) > 0
                        );

                if (!relevant) {
                    continue;
                }

                contextNumber++;

                SourceDocument document = new SourceDocument(
                        "cuad-liability-train-" + contextNumber,
                        "train",
                        context
                );

                List<Span> predicted = new ArrayList<>();

                for (RiskFinding finding :
                        LiabilitySignalFinder.find(
                                document, "agreement"
                        )) {
                    for (EvidenceSpan evidence :
                            finding.evidence()) {
                        int start = context.offsetByCodePoints(
                                0, evidence.start()
                        );
                        int end = context.offsetByCodePoints(
                                0, evidence.end()
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

                for (String label : LABELS) {
                    int questionCount =
                            questionsByLabel.get(label);

                    if (questionCount == 0) {
                        continue;
                    }

                    LabelResult labelResult =
                            results.get(label);
                    List<Span> gold =
                            goldByLabel.get(label);

                    labelResult.paragraphs++;
                    labelResult.questions += questionCount;
                    labelResult.answerSpans += gold.size();

                    ContractResult contractResult =
                            labelResult.contracts
                                    .computeIfAbsent(
                                            title,
                                            ignored ->
                                                    new ContractResult()
                                    );

                    contractResult.goldPositive |=
                            !gold.isEmpty();
                    contractResult.scannerPositive |=
                            !predicted.isEmpty();

                    boolean overlap = gold.stream()
                            .anyMatch(goldSpan ->
                                    predicted.stream()
                                            .anyMatch(
                                                    goldSpan::overlaps
                                            )
                            );

                    contractResult.evidenceOverlap |=
                            overlap;
                }
            }
        }

        System.out.println(
                "CUAD LIABILITY TRAINING CONTRACT AUDIT"
        );
        System.out.println(
                "Source: train_separate_questions.json"
        );
        System.out.println("Held-out test.json: not read");
        System.out.println(
                "Unit: contract title with a target question"
        );

        for (String label : LABELS) {
            printResult(label, results.get(label));
        }

        System.out.println(
                "Interpretation: generic liability cue versus "
                        + "two narrower CUAD labels. These figures "
                        + "describe training-split candidate overlap, "
                        + "not cap/uncapped classification accuracy. "
                        + "Evidence overlap does not establish the "
                        + "legal effect of a clause."
        );
    }

    private static void printResult(
            String label,
            LabelResult result
    ) {
        int bothPositive = 0;
        int scannerOnly = 0;
        int goldOnly = 0;
        int bothNegative = 0;
        int aligned = 0;

        for (ContractResult contract :
                result.contracts.values()) {
            if (contract.goldPositive
                    && contract.scannerPositive) {
                bothPositive++;
                if (contract.evidenceOverlap) {
                    aligned++;
                }
            } else if (contract.scannerPositive) {
                scannerOnly++;
            } else if (contract.goldPositive) {
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

        System.out.println();
        System.out.println(label);
        System.out.println(
                "  Evaluated contracts: "
                        + result.contracts.size()
        );
        System.out.println(
                "  Evaluated contexts: "
                        + result.paragraphs
        );
        System.out.println(
                "  Target question instances: "
                        + result.questions
        );
        System.out.println(
                "  Gold answer spans: "
                        + result.answerSpans
        );
        System.out.println(
                "  Gold-positive contracts: "
                        + (bothPositive + goldOnly)
        );
        System.out.println(
                "  Scanner-positive contracts: "
                        + (bothPositive + scannerOnly)
        );
        System.out.println(
                "  Both positive: " + bothPositive
        );
        System.out.println(
                "  Scanner only: " + scannerOnly
        );
        System.out.println(
                "  Gold only: " + goldOnly
        );
        System.out.println(
                "  Both negative: " + bothNegative
        );
        System.out.println(
                "  Both positive with evidence overlap: "
                        + aligned
        );
        System.out.println(
                "  Both positive without evidence overlap: "
                        + (bothPositive - aligned)
        );

        System.out.printf(
                Locale.ROOT,
                "  Candidate flag precision: %.3f%n"
                        + "  Candidate flag recall: %.3f%n"
                        + "  Candidate flag F1: %.3f%n",
                precision,
                recall,
                f1
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