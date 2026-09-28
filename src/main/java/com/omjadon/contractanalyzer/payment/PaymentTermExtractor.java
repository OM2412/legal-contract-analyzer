package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentTermExtractor {

    private static final Pattern PAYMENT_CLAUSE = Pattern.compile(
            "\\b(?:The\\s+)?(?<payer>Client|Customer)"
                    + "\\s+(?:shall|must)\\s+pay"
                    + "\\s+(?:the\\s+)?(?<payee>Provider|Vendor)"
                    + "\\s+the\\s+(?<fee>project|implementation)"
                    + "\\s+fee"
                    + "\\s+(?:within|no\\s+later\\s+than)"
                    + "\\s+(?<days>\\d+)\\s+"
                    + "(?:(?<unit>calendar|business)\\s+)?days"
                    + "\\s+(?:(?:(?:after|following)"
                    + "\\s+receipt\\s+of"
                    + "|of\\s+receipt\\s+of"
                    + "|after\\s+receiving)"
                    + "\\s+(?:the|an|a)\\s+"
                    + "(?:valid\\s+)?invoice"
                    + "|(?<acceptance>after\\s+final\\s+acceptance)"
                    + "|(?<invoiceDate>after\\s+(?:the\\s+)?invoice"
                    + "\\s+date))\\.",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LINKED_INVOICE_CLAUSE =
            Pattern.compile(
                    "\\bProvider\\s+will\\s+issue\\s+an\\s+invoice"
                            + "\\s+for\\s+the\\s+project\\s+fee\\."
                            + "\\s+Client\\s+must\\s+pay"
                            + "\\s+that\\s+invoice"
                            + "\\s+within\\s+(?<days>\\d+)\\s+"
                            + "(?:(?<unit>calendar|business)\\s+)?days"
                            + "\\s+of\\s+receiving\\s+it\\.",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern DUE_AND_PAYABLE_CLAUSE =
            Pattern.compile(
                    "\\bThe\\s+project\\s+fee\\s+is\\s+due\\s+and"
                            + "\\s+payable\\s+by\\s+Client"
                            + "\\s+to\\s+Provider"
                            + "\\s+within\\s+(?<days>\\d+)\\s+"
                            + "(?:(?<unit>calendar|business)\\s+)?days"
                            + "\\s+after\\s+receipt\\s+of\\s+"
                            + "(?:the|an)\\s+invoice\\.",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern PASSIVE_PAYMENT_CLAUSE =
            Pattern.compile(
                    "\\b(?<payee>Provider|Vendor)"
                            + "\\s+shall\\s+be\\s+paid"
                            + "\\s+the\\s+(?<fee>project|implementation)"
                            + "\\s+fee\\s+by\\s+(?<payer>Client|Customer)"
                            + "\\s+(?:within|no\\s+later\\s+than)"
                            + "\\s+(?<days>\\d+)\\s+"
                            + "(?:(?<unit>calendar|business)\\s+)?days"
                            + "\\s+after\\s+(?:the\\s+)?\\k<payer>"
                            + "\\s+receives\\s+(?:a|an|the)\\s+"
                            + "(?:valid\\s+)?invoice\\.",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern INSTALLMENT_CLAUSE =
            Pattern.compile(
                    "\\b(?<payer>Client|Customer)"
                            + "\\s+(?:shall|must)\\s+pay"
                            + "\\s+(?:the\\s+)?(?<payee>Provider|Vendor)"
                            + "\\s+(?:the\\s+remaining\\s+)?"
                            + "(?<percent>100|[1-9]\\d?)%"
                            + "\\s+of\\s+the"
                            + "\\s+(?<fee>project|implementation)"
                            + "\\s+fee"
                            + "\\s+(?:within|no\\s+later\\s+than)"
                            + "\\s+(?<days>\\d+)\\s+"
                            + "(?:(?<unit>calendar|business)\\s+)?days"
                            + "\\s+after\\s+(?:"
                            + "(?<acceptance>final\\s+acceptance"
                            + "\\s+of\\s+the\\s+deliverables)"
                            + "|signing\\s+this\\s+Agreement"
                            + ")\\.",
                    Pattern.CASE_INSENSITIVE
            );

    private PaymentTermExtractor() {
    }

    public static List<PaymentTerm> extractAll(SourceDocument document) {
        Objects.requireNonNull(document, "document");

        List<PaymentTerm> terms = new ArrayList<>();

        addMatches(document, PAYMENT_CLAUSE, false, false, false, terms);
        addMatches(document, LINKED_INVOICE_CLAUSE, true, true, false, terms);
        addMatches(document, DUE_AND_PAYABLE_CLAUSE, true, true, false, terms);
        addMatches(document, PASSIVE_PAYMENT_CLAUSE, true, false, false, terms);
        addMatches(document, INSTALLMENT_CLAUSE, false, false, true, terms);

        terms.sort(Comparator.comparingInt(
                term -> term.evidence().start()
        ));

        return List.copyOf(terms);
    }

    private static void addMatches(
            SourceDocument document,
            Pattern pattern,
            boolean fixedInvoiceReceipt,
            boolean fixedPartiesAndScope,
            boolean installment,
            List<PaymentTerm> terms
    ) {
        Matcher matcher = pattern.matcher(document.text());

        while (matcher.find()) {
            final int days;

            try {
                days = Integer.parseInt(matcher.group("days"));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Payment period is too large to represent",
                        exception
                );
            }

            String unitText = matcher.group("unit");
            PaymentTerm.DayUnit unit;

            if (unitText == null) {
                unit = PaymentTerm.DayUnit.UNKNOWN;
            } else if (unitText.equalsIgnoreCase("business")) {
                unit = PaymentTerm.DayUnit.BUSINESS_DAYS;
            } else {
                unit = PaymentTerm.DayUnit.CALENDAR_DAYS;
            }

            PaymentTerm.PaymentTrigger trigger;

            if (installment) {
                trigger = matcher.group("acceptance") != null
                        ? PaymentTerm.PaymentTrigger.FINAL_ACCEPTANCE
                        : PaymentTerm.PaymentTrigger.OTHER;
            } else if (fixedInvoiceReceipt) {
                trigger = PaymentTerm.PaymentTrigger.INVOICE_RECEIPT;
            } else if (matcher.group("acceptance") != null) {
                trigger = PaymentTerm.PaymentTrigger.FINAL_ACCEPTANCE;
            } else if (matcher.group("invoiceDate") != null) {
                trigger = PaymentTerm.PaymentTrigger.INVOICE_DATE;
            } else {
                trigger = PaymentTerm.PaymentTrigger.INVOICE_RECEIPT;
            }

            String scope = fixedPartiesAndScope
                    ? "project_fee"
                    : matcher.group("fee")
                            .toLowerCase(Locale.ROOT) + "_fee";

            if (installment) {
                scope += "_" + matcher.group("percent") + "_percent";
            }

            String payer = fixedPartiesAndScope
                    ? "Client"
                    : matcher.group("payer");
            String payee = fixedPartiesAndScope
                    ? "Provider"
                    : matcher.group("payee");

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
                    scope,
                    payer,
                    payee,
                    evidence
            ));
        }
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