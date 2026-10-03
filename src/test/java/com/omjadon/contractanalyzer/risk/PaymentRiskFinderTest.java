package com.omjadon.contractanalyzer.risk;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;
import com.omjadon.contractanalyzer.payment.PaymentPolicy;
import com.omjadon.contractanalyzer.payment.PaymentReviewService;
import com.omjadon.contractanalyzer.payment.PaymentTerm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentRiskFinderTest {

    private static final PaymentPolicy POLICY = new PaymentPolicy(
            "P-DEMO-30",
            "1.0",
            30,
            PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
    );

    private static final String AGREEMENT_CLAUSE =
            "Client shall pay Provider the project fee within "
                    + "30 calendar days after receipt of the invoice.";

    private static final String SOW_CLAUSE =
            "Client shall pay Provider the project fee within "
                    + "60 calendar days after receipt of the invoice.";

    @Test
    void differingTermsProduceSourceBackedFindings() {
        SourceDocument agreement = new SourceDocument(
                "agreement", "1", AGREEMENT_CLAUSE
        );
        SourceDocument sow = new SourceDocument(
                "sow", "1", SOW_CLAUSE
        );

        List<RiskFinding> findings = find(agreement, sow);

        assertEquals(2, findings.size());
        assertTrue(findings.stream().anyMatch(finding ->
                finding.signal()
                        == RiskFinding.Signal.DOCUMENT_DIFFERENCE
        ));
        assertTrue(findings.stream().anyMatch(finding ->
                finding.signal()
                        == RiskFinding.Signal.POLICY_DEVIATION
        ));

        RiskFinding difference = findings.stream()
                .filter(finding -> finding.signal()
                        == RiskFinding.Signal.DOCUMENT_DIFFERENCE)
                .findFirst()
                .orElseThrow();

        assertEquals(2, difference.evidence().size());
        assertEquals(
                AGREEMENT_CLAUSE,
                difference.evidence().get(0).quote()
        );
        assertEquals(
                SOW_CLAUSE,
                difference.evidence().get(1).quote()
        );
        EvidenceValidator.validate(
                agreement, difference.evidence().get(0)
        );
        EvidenceValidator.validate(
                sow, difference.evidence().get(1)
        );

        assertTrue(findings.stream().allMatch(finding ->
                finding.priority() == RiskFinding.Priority.REVIEW_ONLY
        ));
    }

    @Test
    void negatedPaymentDoesNotBecomeAFinding() {
        SourceDocument agreement = new SourceDocument(
                "agreement",
                "1",
                "This Agreement does not require Customer to pay "
                        + "any implementation fee."
        );
        SourceDocument sow = new SourceDocument(
                "sow", "1", AGREEMENT_CLAUSE
        );

        assertTrue(find(agreement, sow).isEmpty());
    }

    @Test
    void unparsedPaymentWordingIsFlaggedWithItsSource() {
        String wording =
                "Client shall pay Provider the project fee within "
                        + "ninety (90) calendar days after receipt "
                        + "of the invoice.";

        SourceDocument agreement = new SourceDocument(
                "agreement", "1", wording
        );
        SourceDocument sow = new SourceDocument(
                "sow", "1", AGREEMENT_CLAUSE
        );

        List<RiskFinding> findings = find(agreement, sow);

        assertEquals(1, findings.size());
        assertEquals(
                RiskFinding.Signal.COVERAGE_GAP,
                findings.get(0).signal()
        );
        assertEquals(wording, findings.get(0).evidence().get(0).quote());
        EvidenceValidator.validate(
                agreement, findings.get(0).evidence().get(0)
        );
    }

    private static List<RiskFinding> find(
            SourceDocument agreement,
            SourceDocument sow
    ) {
        PaymentReviewService.Review review =
                PaymentReviewService.analyze(agreement, sow, POLICY);

        return PaymentRiskFinder.find(agreement, sow, review);
    }
}