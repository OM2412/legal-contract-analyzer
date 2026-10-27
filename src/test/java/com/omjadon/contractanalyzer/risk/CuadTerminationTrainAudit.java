package com.omjadon.contractanalyzer.risk;

import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.json.JsonParserFactory;

/** Exploratory evidence audit on CUAD training data only. */
public final class CuadTerminationTrainAudit {
    private static final String LABEL = "\"Termination For Convenience\"";
    private static final Path TRAIN = Path.of(
            "data", "external", "cuad", "train_separate_questions.json"
    );

    private record Span(int start, int end, String quote) {
        boolean overlaps(Span other) {
            return start < other.end && other.start < end;
        }
    }

    private CuadTerminationTrainAudit() {
    }

    public static void main(String[] args) throws Exception {
        if (!Files.isRegularFile(TRAIN)) {
            throw new IllegalStateException(
                    "CUAD training file missing: " + TRAIN
            );
        }

        Map<String, Object> root = JsonParserFactory.getJsonParser()
                .parseMap(Files.readString(TRAIN, StandardCharsets.UTF_8));

        int evaluated = 0;
        int questionInstances = 0;
        int answerSpans = 0;
        int bothPositive = 0;
        int scannerOnly = 0;
        int goldOnly = 0;
        int bothNegative = 0;
        int alignedUnits = 0;
        int unalignedUnits = 0;
        List<String> missedExamples = new ArrayList<>();
        List<String> unalignedExamples = new ArrayList<>();

        for (Object entry : list(root.get("data"), "data")) {
            Map<?, ?> contract = map(entry, "contract");
            String title = string(contract.get("title"), "title");

            for (Object paragraphObject : list(
                    contract.get("paragraphs"), "paragraphs")) {
                Map<?, ?> paragraph = map(paragraphObject, "paragraph");
                String context = string(paragraph.get("context"), "context");
                List<Span> gold = new ArrayList<>();
                int relevantQuestions = 0;

                for (Object questionObject : list(
                        paragraph.get("qas"), "qas")) {
                    Map<?, ?> question = map(questionObject, "question");
                    String wording = string(
                            question.get("question"), "question text"
                    );

                    if (!wording.contains(LABEL)) {
                        continue;
                    }

                    relevantQuestions++;
                    questionInstances++;

                    for (Object answerObject : list(
                            question.get("answers"), "answers")) {
                        Map<?, ?> answer = map(answerObject, "answer");
                        String quote = string(
                                answer.get("text"), "answer text"
                        );
                        int start = number(
                                answer.get("answer_start"), "answer_start"
                        );
                        int end = start + quote.length();

                        if (start < 0 || end > context.length()
                                || !context.substring(start, end)
                                        .equals(quote)) {
                            throw new IllegalStateException(
                                    "Gold answer offset mismatch in " + title
                            );
                        }

                        gold.add(new Span(start, end, quote));
                        answerSpans++;
                    }
                }

                if (relevantQuestions == 0) {
                    continue;
                }

                evaluated++;
                SourceDocument document = new SourceDocument(
                        "cuad-train-" + evaluated, "train", context
                );

                List<Span> predicted = new ArrayList<>();

                for (RiskFinding finding : TerminationSignalFinder.find(
                        document, "agreement")) {
                    for (EvidenceSpan evidence : finding.evidence()) {
                        int start = context.offsetByCodePoints(
                                0, evidence.start()
                        );
                        int end = context.offsetByCodePoints(
                                0, evidence.end()
                        );
                        predicted.add(
                                new Span(start, end, evidence.quote())
                        );
                    }
                }

                if (!gold.isEmpty() && !predicted.isEmpty()) {
                    bothPositive++;

                    boolean overlaps = gold.stream().anyMatch(g ->
                            predicted.stream().anyMatch(g::overlaps));

                    if (overlaps) {
                        alignedUnits++;
                    } else {
                        unalignedUnits++;

                        if (unalignedExamples.size() < 3) {
                            unalignedExamples.add(
                                    title
                                            + " | gold: "
                                            + preview(gold.get(0).quote())
                                            + " | signal: "
                                            + preview(
                                                    predicted.get(0).quote()
                                            )
                            );
                        }
                    }
                } else if (!gold.isEmpty()) {
                    goldOnly++;

                    if (missedExamples.size() < 5) {
                        missedExamples.add(
                                title + " | "
                                        + preview(gold.get(0).quote())
                        );
                    }
                } else if (!predicted.isEmpty()) {
                    scannerOnly++;
                } else {
                    bothNegative++;
                }
            }
        }

        System.out.println(
                "CUAD TERMINATION TRAINING EVIDENCE AUDIT"
        );
        System.out.println(
                "Source: train_separate_questions.json"
        );
        System.out.println(
                "Held-out test.json: not read"
        );
        System.out.println(
                "Unit: paragraph/document with a target question"
        );
        System.out.println("Evaluated units: " + evaluated);
        System.out.println(
                "Target question instances: " + questionInstances
        );
        System.out.println("Gold answer spans: " + answerSpans);
        System.out.println("Both positive: " + bothPositive);
        System.out.println("Scanner only: " + scannerOnly);
        System.out.println("Gold only: " + goldOnly);
        System.out.println("Both negative: " + bothNegative);
        System.out.println(
                "Both positive, at least one evidence overlap: "
                        + alignedUnits
        );
        System.out.println(
                "Both positive, no evidence overlap: "
                        + unalignedUnits
        );

        System.out.println(
                "Selected gold-only training examples:"
        );
        missedExamples.forEach(
                example -> System.out.println("  " + example)
        );

        System.out.println(
                "Selected unaligned training examples:"
        );
        unalignedExamples.forEach(
                example -> System.out.println("  " + example)
        );

        System.out.println(
                "Exploratory only: generic termination signal versus "
                        + "CUAD's narrower without-cause label. "
                        + "Evidence overlap does not establish a "
                        + "correct clause classification."
        );
    }

    private static String preview(String text) {
        String compact = text.replaceAll("\\s+", " ").strip();
        return compact.length() > 180
                ? compact.substring(0, 180) + "..."
                : compact;
    }

    private static Map<?, ?> map(Object value, String field) {
        if (!(value instanceof Map<?, ?> result)) {
            throw new IllegalArgumentException(
                    "Expected object: " + field
            );
        }
        return result;
    }

    private static List<?> list(Object value, String field) {
        if (!(value instanceof List<?> result)) {
            throw new IllegalArgumentException(
                    "Expected array: " + field
            );
        }
        return result;
    }

    private static String string(Object value, String field) {
        if (!(value instanceof String result)) {
            throw new IllegalArgumentException(
                    "Expected string: " + field
            );
        }
        return result;
    }

    private static int number(Object value, String field) {
        if (!(value instanceof Number result)) {
            throw new IllegalArgumentException(
                    "Expected number: " + field
            );
        }
        return result.intValue();
    }
}