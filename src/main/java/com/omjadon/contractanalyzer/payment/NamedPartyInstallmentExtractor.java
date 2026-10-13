package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts explicit installments and a narrow adjacent remainder. */
final class NamedPartyInstallmentExtractor {

    private static final String SPACE = "[ \\t\\r\\n]+";

    private static final String WORD =
            "[\\p{L}][\\p{L}\\p{N}&,'\\u2019-]*";

    private static final String PERCENT = "(?:100|[1-9]\\d?)";

    private static final Pattern CLAUSE = Pattern.compile(
            "\\b(?<payer>Client|Customer)"
                    + SPACE + "(?:shall|must)"
                    + SPACE + "pay"
                    + SPACE + "(?<payee>" + WORD
                    + "(?:" + SPACE + WORD + "){0,5}?)"
                    + SPACE + "(?<scope>(?<percent>" + PERCENT + ")"
                    + "(?:" + SPACE + "percent|%)"
                    + SPACE + "of"
                    + SPACE + "the"
                    + SPACE + "(?<feeName>" + WORD
                    + "(?:" + SPACE + WORD + "){0,5}?)"
                    + SPACE + "fee)"
                    + SPACE + "(?:within|no"
                    + SPACE + "later" + SPACE + "than)"
                    + SPACE + "(?<days>\\d+)"
                    + SPACE
                    + "(?:(?<unit>calendar|business)" + SPACE + ")?days"
                    + SPACE + "after"
                    + SPACE + "(?:the" + SPACE + ")?invoice"
                    + SPACE + "date\\.",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    // Allow one sentence gap with at most one line break.
    // Do not link across a blank paragraph or a page break.
    private static final String ADJACENT_GAP =
            "(?:[ \\t]+|[ \\t]*\\r?\\n[ \\t]*)";

    private static final Pattern REMAINING = Pattern.compile(
            ADJACENT_GAP
                    + "(?<payer>Client|Customer)"
                    + SPACE + "(?:shall|must)"
                    + SPACE + "pay"
                    + SPACE + "(?<payee>" + WORD
                    + "(?:" + SPACE + WORD + "){0,5}?)"
                    + SPACE + "the"
                    + SPACE + "remaining"
                    + SPACE + "(?<percent>" + PERCENT + ")"
                    + "(?:" + SPACE + "percent|%)"
                    + SPACE + "(?:within|no"
                    + SPACE + "later" + SPACE + "than)"
                    + SPACE + "(?<days>\\d+)"
                    + SPACE
                    + "(?:(?<unit>calendar|business)" + SPACE + ")?days"
                    + SPACE + "after"
                    + SPACE + "(?<receiver>Client|Customer)"
                    + SPACE + "gives"
                    + SPACE + "final"
                    + SPACE + "written"
                    + SPACE + "acceptance"
                    + SPACE + "of"
                    + SPACE + "the"
                    + SPACE + "(?<deliverableName>" + WORD
                    + "(?:" + SPACE + WORD + "){0,5}?)"
                    + SPACE + "deliverable\\.",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private NamedPartyInstallmentExtractor() {
    }

    static List<PaymentTerm> extractAll(SourceDocument document) {
        Objects.requireNonNull(document, "document");

        String text = document.text();
        Matcher first = CLAUSE.matcher(text);
        List<PaymentTerm> terms = new ArrayList<>();

        while (first.find()) {
            String payer = normalize(first.group("payer"));
            String payee = normalize(first.group("payee"));

            String feeName = normalize(first.group("feeName"))
                    .toLowerCase(Locale.ROOT);

            int firstPercent = Integer.parseInt(
                    first.group("percent")
            );

            terms.add(term(
                    document,
                    first,
                    payer,
                    payee,
                    explicitScope(firstPercent, feeName),
                    PaymentTerm.PaymentTrigger.INVOICE_DATE,
                    first.start(),
                    first.end()
            ));

            Matcher remaining = REMAINING.matcher(text);
            remaining.region(first.end(), text.length());

            if (!remaining.lookingAt()) {
                continue;
            }

            int remainingPercent = Integer.parseInt(
                    remaining.group("percent")
            );

            if (!payer.equalsIgnoreCase(normalize(
                    remaining.group("payer")))
                    || !payee.equalsIgnoreCase(normalize(
                            remaining.group("payee")))
                    || !payer.equalsIgnoreCase(normalize(
                            remaining.group("receiver")))
                    || firstPercent + remainingPercent != 100
                    || !feeName.equalsIgnoreCase(normalize(
                            remaining.group("deliverableName")))) {
                continue;
            }

            // Both sentences support the inherited fee scope.
            terms.add(term(
                    document,
                    remaining,
                    payer,
                    payee,
                    "remaining " + remainingPercent
                            + " percent of the " + feeName + " fee",
                    PaymentTerm.PaymentTrigger.FINAL_ACCEPTANCE,
                    first.start(),
                    remaining.end()
            ));
        }

        return List.copyOf(terms);
    }

    private static PaymentTerm term(
            SourceDocument document,
            Matcher matcher,
            String payer,
            String payee,
            String scope,
            PaymentTerm.PaymentTrigger trigger,
            int utf16Start,
            int utf16End
    ) {
        final int days;

        try {
            days = Integer.parseInt(matcher.group("days"));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "Payment period is too large to represent",
                    error
            );
        }

        String unitText = matcher.group("unit");

        PaymentTerm.DayUnit unit = unitText == null
                ? PaymentTerm.DayUnit.UNKNOWN
                : unitText.equalsIgnoreCase("business")
                    ? PaymentTerm.DayUnit.BUSINESS_DAYS
                    : PaymentTerm.DayUnit.CALENDAR_DAYS;

        int start = document.text().codePointCount(0, utf16Start);
        int end = document.text().codePointCount(0, utf16End);

        return new PaymentTerm(
                days,
                unit,
                trigger,
                scope,
                payer,
                payee,
                EvidenceValidator.fromRange(document, start, end)
        );
    }

    private static String explicitScope(
            int percent,
            String feeName
    ) {
        if (feeName.equals("project")
                || feeName.equals("implementation")) {
            return feeName + "_fee_" + percent + "_percent";
        }

        return percent + " percent of the " + feeName + " fee";
    }

    private static String normalize(String value) {
        return value.strip().replaceAll("[ \\t\\r\\n]+", " ");
    }
}