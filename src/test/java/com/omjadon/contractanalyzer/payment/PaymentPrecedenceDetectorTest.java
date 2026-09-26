package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentPrecedenceDetectorTest {

    private static final String SOW_RULE =
            "If this Agreement and the SOW differ on payment "
                    + "for the project fee, the SOW payment term prevails.";

    private static final String AGREEMENT_RULE =
            "If this Agreement and the SOW differ on payment "
                    + "for the project fee, the Agreement payment term prevails.";

    @Test
    void locatesSowPriorityWithExactEvidence() {
        SourceDocument agreement = new SourceDocument(
                "agreement-test",
                "1",
                "Payment terms.\n" + SOW_RULE
        );

        SourceDocument sow = new SourceDocument(
                "sow-test",
                "1",
                "Project fee: 60 calendar days."
        );

        PaymentPrecedenceDetector.Detection result =
                PaymentPrecedenceDetector.detect(agreement, sow);

        assertEquals(
                PaymentPrecedenceDetector.Status.SOW_TEXT_PRIORITY,
                result.status()
        );
        assertEquals(1, result.evidence().size());
        assertEquals(SOW_RULE, result.evidence().getFirst().quote());

        EvidenceValidator.validate(
                agreement,
                result.evidence().getFirst()
        );
    }

    @Test
    void doesNotSelectOneOfTwoOpposingRules() {
        SourceDocument agreement = new SourceDocument(
                "agreement-test",
                "1",
                SOW_RULE + "\n" + AGREEMENT_RULE
        );

        SourceDocument sow = new SourceDocument(
                "sow-test",
                "1",
                "Payment schedule."
        );

        PaymentPrecedenceDetector.Detection result =
                PaymentPrecedenceDetector.detect(agreement, sow);

        assertEquals(
                PaymentPrecedenceDetector.Status.MULTIPLE_RULES,
                result.status()
        );
        assertEquals(2, result.evidence().size());
    }

    @Test
    void reportsOnlyThatItsSupportedWordingWasNotLocated() {
        SourceDocument agreement = new SourceDocument(
                "agreement-test",
                "1",
                "The parties agree to the project."
        );

        SourceDocument sow = new SourceDocument(
                "sow-test",
                "1",
                "Payment schedule attached."
        );

        PaymentPrecedenceDetector.Detection result =
                PaymentPrecedenceDetector.detect(agreement, sow);

        assertEquals(
                PaymentPrecedenceDetector.Status.NOT_LOCATED,
                result.status()
        );
        assertTrue(result.evidence().isEmpty());
    }
}