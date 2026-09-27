package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentTermExtractor {

    private static final Pattern PAYMENT_CLAUSE = Pattern.compile(
            "\\b(?:The\\s+)?Client\\s+(?:shall|must)\\s+pay"
                    + "\\s+(?:the\\s+)?Provider\\s+the\\s+project\\s+fee"
                    + "\\s+(?:within|no\\s+later\\s+than)\\s+(\\d+)\\s+"
                    + "(?:(calendar|business)\\s+)?days"
                    + "\\s+(?:(?:(?:after|following)\\s+receipt\\s+of"
                    + "|of\\s+receipt\\s+of"
                    + "|after\\s+receiving)\\s+(?:the|an)\\s+invoice"
                    + "|(?<acceptance>after\\s+final\\s+acceptance)"
                    + "|(?<invoiceDate>after\\s+(?:the\\s+)?invoice\\s+date))\\.",
            Pattern.CASE_INSENSITIVE
    );

    private PaymentTermExtractor() {
    }

    public static List<PaymentTerm> extractAll(SourceDocument document) {
        Objects.requireNonNull(document, "document");

        Matcher matcher = PAYMENT_CLAUSE.matcher(document.text());
        List<PaymentTerm> terms = new ArrayList<>();

        while (matcher.find()) {
            final int days;

            try {
                days = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Payment period is too large to represent",
                        exception
                );
            }

            String unitText = matcher.group(2);
            PaymentTerm.DayUnit unit;

            if (unitText == null) {
                unit = PaymentTerm.DayUnit.UNKNOWN;
            } else if (unitText.equalsIgnoreCase("business")) {
                unit = PaymentTerm.DayUnit.BUSINESS_DAYS;
            } else {
                unit = PaymentTerm.DayUnit.CALENDAR_DAYS;
            }

            PaymentTerm.PaymentTrigger trigger;

            if (matcher.group("acceptance") != null) {
                trigger = PaymentTerm.PaymentTrigger.FINAL_ACCEPTANCE;
            } else if (matcher.group("invoiceDate") != null) {
                trigger = PaymentTerm.PaymentTrigger.INVOICE_DATE;
            } else {
                trigger = PaymentTerm.PaymentTrigger.INVOICE_RECEIPT;
            }

            int start = document.text().codePointCount(
                    0, matcher.start()
            );
            int end = document.text().codePointCount(
                    0, matcher.end()
            );

            EvidenceSpan evidence = EvidenceValidator.fromRange(
                    document, start, end
            );

            terms.add(new PaymentTerm(
                    days,
                    unit,
                    trigger,
                    "project_fee",
                    "Client",
                    "Provider",
                    evidence
            ));
        }

        return List.copyOf(terms);
    }

    public static Optional<PaymentTerm> extract(SourceDocument document) {
        List<PaymentTerm> terms = extractAll(document);

        if (terms.size() > 1) {
            throw new IllegalArgumentException(
                    "Multiple matching payment clauses in "
                            + document.documentId()
            );
        }

        return terms.stream().findFirst();
    }
}