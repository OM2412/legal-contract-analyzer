package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Extracts explicit percentage-of-fee invoice-date obligations. */
final class NamedPartyInstallmentExtractor {

    private static final String SPACE = "[ \\t\\r\\n]+";

    private static final String WORD =
            "[\\p{L}][\\p{L}\\p{N}&,'\\u2019-]*";

    private static final Pattern CLAUSE = Pattern.compile(
            "\\b(?<payer>Client|Customer)"
                    + SPACE + "(?:shall|must)"
                    + SPACE + "pay"
                    + SPACE
                    + "(?<payee>" + WORD
                    + "(?:" + SPACE + WORD + "){0,5}?)"
                    + SPACE
                    + "(?<scope>(?<percent>100|[1-9]\\d?)"
                    + "(?:" + SPACE + "percent|%)"
                    + SPACE + "of"
                    + SPACE + "the"
                    + SPACE + WORD
                    + "(?:" + SPACE + WORD + "){0,5}?"
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

    private NamedPartyInstallmentExtractor() {
    }

    static List<PaymentTerm> extractAll(SourceDocument document) {
        Objects.requireNonNull(document, "document");

        String text = document.text();
        var matcher = CLAUSE.matcher(text);
        List<PaymentTerm> terms = new ArrayList<>();

        while (matcher.find()) {
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

            String scope = normalize(
                    matcher.group("scope").replace("%", " percent")
            ).toLowerCase(Locale.ROOT);

            String percent = matcher.group("percent");

            if (scope.equals(percent + " percent of the project fee")) {
                scope = "project_fee_" + percent + "_percent";
            } else if (scope.equals(
                    percent + " percent of the implementation fee"
            )) {
                scope = "implementation_fee_" + percent + "_percent";
            }

            int start = text.codePointCount(0, matcher.start());
            int end = text.codePointCount(0, matcher.end());

            terms.add(new PaymentTerm(
                    days,
                    unit,
                    PaymentTerm.PaymentTrigger.INVOICE_DATE,
                    scope,
                    normalize(matcher.group("payer")),
                    normalize(matcher.group("payee")),
                    EvidenceValidator.fromRange(document, start, end)
            ));
        }

        return List.copyOf(terms);
    }

    private static String normalize(String value) {
        return value.strip().replaceAll("[ \\t\\r\\n]+", " ");
    }
}