package com.omjadon.contractanalyzer.ai;

import com.omjadon.contractanalyzer.model.EvidenceSpan;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class AiPaymentDetailVerifier {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final Pattern PERIOD = Pattern.compile(
            "\\b([1-9]\\d{0,3})\\s+(calendar|business)\\s+days?\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EXPLICIT_INVOICE_RECEIPT =
            Pattern.compile(
                    "\\b(?:receipt\\s+of\\s+(?:the|an)\\s+invoice"
                            + "|receiv(?:e|es|ed|ing)\\s+"
                            + "(?:the|an|that)\\s+invoice)\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private AiPaymentDetailVerifier() {
    }

    public static Optional<Candidate> verify(
            String modelJson,
            EvidenceSpan verifiedEvidence
    ) {
        Objects.requireNonNull(modelJson, "modelJson");
        Objects.requireNonNull(verifiedEvidence, "verifiedEvidence");

        if (modelJson.length() > 20_000) {
            return Optional.empty();
        }

        final JsonNode root;

        try {
            root = JSON.readTree(modelJson);
        } catch (JacksonException exception) {
            return Optional.empty();
        }

        if (root == null
                || !root.isObject()
                || root.size() != 3
                || !root.has("days")
                || !root.has("dayUnit")
                || !root.has("trigger")) {
            return Optional.empty();
        }

        JsonNode daysNode = root.get("days");
        JsonNode unitNode = root.get("dayUnit");
        JsonNode triggerNode = root.get("trigger");

        if (!daysNode.isIntegralNumber()
                || !daysNode.canConvertToInt()
                || !unitNode.isString()
                || !(triggerNode.isNull()
                        || triggerNode.isString()
                        && "INVOICE_RECEIPT".equals(
                                triggerNode.stringValue()
                        ))) {
            return Optional.empty();
        }

        int claimedDays = daysNode.intValue();
        String claimedUnit = unitNode.stringValue();

        if (claimedDays < 1
                || claimedDays > 3650
                || !(claimedUnit.equals("CALENDAR_DAYS")
                        || claimedUnit.equals("BUSINESS_DAYS"))) {
            return Optional.empty();
        }

        Matcher period = PERIOD.matcher(verifiedEvidence.quote());

        // Multiple periods in one quote are ambiguous.
        if (!period.find()) {
            return Optional.empty();
        }

        int quotedDays = Integer.parseInt(period.group(1));
        String quotedUnit = period.group(2).equalsIgnoreCase("calendar")
                ? "CALENDAR_DAYS"
                : "BUSINESS_DAYS";

        if (period.find()
                || claimedDays != quotedDays
                || !claimedUnit.equals(quotedUnit)) {
            return Optional.empty();
        }

        boolean invoiceReceiptConfirmed =
                triggerNode.isString()
                        && EXPLICIT_INVOICE_RECEIPT.matcher(
                                verifiedEvidence.quote()
                        ).find();

        return Optional.of(new Candidate(
                claimedDays,
                claimedUnit,
                invoiceReceiptConfirmed,
                verifiedEvidence
        ));
    }

    public record Candidate(
            int days,
            String dayUnit,
            boolean invoiceReceiptConfirmed,
            EvidenceSpan evidence
    ) {
    }
}