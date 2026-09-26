package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentPrecedenceDetector {

    private static final Pattern SOW_PRIORITY = Pattern.compile(
            "If this Agreement and the SOW differ on payment "
                    + "for the project fee,\\s+the SOW payment term prevails\\.",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern AGREEMENT_PRIORITY = Pattern.compile(
            "If this Agreement and the SOW differ on payment "
                    + "for the project fee,\\s+the Agreement payment term prevails\\.",
            Pattern.CASE_INSENSITIVE
    );

    private PaymentPrecedenceDetector() {
    }

    public enum Status {
        SOW_TEXT_PRIORITY,
        AGREEMENT_TEXT_PRIORITY,
        MULTIPLE_RULES,
        NOT_LOCATED
    }

    public record Detection(Status status, List<EvidenceSpan> evidence) {
        public Detection {
            Objects.requireNonNull(status, "status");
            evidence = List.copyOf(evidence);
        }
    }

    public static Detection detect(
            SourceDocument agreement,
            SourceDocument sow
    ) {
        Objects.requireNonNull(agreement, "agreement");
        Objects.requireNonNull(sow, "sow");

        List<EvidenceSpan> sowRules = new ArrayList<>();
        List<EvidenceSpan> agreementRules = new ArrayList<>();

        for (SourceDocument document : List.of(agreement, sow)) {
            collectMatches(document, SOW_PRIORITY, sowRules);
            collectMatches(document, AGREEMENT_PRIORITY, agreementRules);
        }

        List<EvidenceSpan> allRules = new ArrayList<>(sowRules);
        allRules.addAll(agreementRules);

        if (allRules.isEmpty()) {
            return new Detection(Status.NOT_LOCATED, List.of());
        }

        if (allRules.size() > 1) {
            return new Detection(Status.MULTIPLE_RULES, allRules);
        }

        return new Detection(
                sowRules.isEmpty()
                        ? Status.AGREEMENT_TEXT_PRIORITY
                        : Status.SOW_TEXT_PRIORITY,
                allRules
        );
    }

    private static void collectMatches(
            SourceDocument document,
            Pattern pattern,
            List<EvidenceSpan> matches
    ) {
        Matcher matcher = pattern.matcher(document.text());

        while (matcher.find()) {
            int start = document.text().codePointCount(0, matcher.start());
            int end = document.text().codePointCount(0, matcher.end());

            matches.add(
                    EvidenceValidator.fromRange(document, start, end)
            );
        }
    }
}