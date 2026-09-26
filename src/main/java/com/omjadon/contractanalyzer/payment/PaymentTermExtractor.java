package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentTermExtractor {

    private static final Pattern PAYMENT_CLAUSE = Pattern.compile(
            "\\bClient\\s+shall\\s+pay\\s+Provider\\s+the\\s+project\\s+fee"
                    + "\\s+within\\s+(\\d+)\\s+"
                    + "(?:(calendar|business)\\s+)?days"
                    + "\\s+(?:after|following)\\s+receipt\\s+of"
                    + "\\s+(?:the|an)\\s+invoice\\.",
            Pattern.CASE_INSENSITIVE
    );

    private PaymentTermExtractor() {
    }

    public static Optional<PaymentTerm> extract(SourceDocument document) {
        Objects.requireNonNull(document, "document");

        Matcher matcher = PAYMENT_CLAUSE.matcher(document.text());

        if (!matcher.find()) {
            return Optional.empty();
        }

        // Record the first match before checking for another clause.
        int startIndex = matcher.start();
        int endIndex = matcher.end();
        String daysText = matcher.group(1);
        String unitText = matcher.group(2);

        if (matcher.find()) {
            throw new IllegalArgumentException(
                    "Multiple matching payment clauses in "
                            + document.documentId()
            );
        }

        final int days;

        try {
            days = Integer.parseInt(daysText);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Payment period is too large to represent",
                    exception
            );
        }

        PaymentTerm.DayUnit unit;

        if (unitText == null) {
            unit = PaymentTerm.DayUnit.UNKNOWN;
        } else if (unitText.equalsIgnoreCase("business")) {
            unit = PaymentTerm.DayUnit.BUSINESS_DAYS;
        } else {
            unit = PaymentTerm.DayUnit.CALENDAR_DAYS;
        }

        int start = document.text().codePointCount(0, startIndex);
        int end = document.text().codePointCount(0, endIndex);

        EvidenceSpan evidence = EvidenceValidator.fromRange(
                document,
                start,
                end
        );

        return Optional.of(new PaymentTerm(
                days,
                unit,
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT,
                "project_fee",
                "Client",
                "Provider",
                evidence
        ));
    }
}