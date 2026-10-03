package com.omjadon.contractanalyzer.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class LiabilitySignalFinderTest {

    @Test
    void capWordingHasExactSourceEvidenceAndReviewOnlyPriority() {
        String clause = "The aggregate liability of either party under "
                + "this Agreement shall not exceed the fees paid.";

        SourceDocument document = new SourceDocument(
                "agreement", "version-1", "📄 " + clause
        );

        List<RiskFinding> findings = LiabilitySignalFinder.find(
                document, "agreement"
        );

        assertEquals(1, findings.size());

        RiskFinding finding = findings.get(0);
        assertEquals(
                RiskFinding.Category.LIABILITY,
                finding.category()
        );
        assertEquals(
                RiskFinding.Signal.CLAUSE_FOR_REVIEW,
                finding.signal()
        );
        assertEquals(
                RiskFinding.Priority.REVIEW_ONLY,
                finding.priority()
        );

        EvidenceSpan span = finding.evidence().get(0);
        EvidenceValidator.validate(document, span);

        assertEquals("📄 " + clause, span.quote());
        assertEquals(
                document.text().codePointCount(
                        0, document.text().length()
                ),
                span.end()
        );
    }

    @Test
    void carveOutIsFlaggedWithoutClaimingItIsUncapped() {
        String clause = "This Subsection will not limit the liability "
                + "of a Party for intentional misconduct.";

        assertSingleQuote(clause);
    }

    @Test
    void excludedLossesAreFlaggedOnlyOncePerSentence() {
        String clause = "In no event shall either party be liable for "
                + "indirect damages, and neither party shall be liable "
                + "for lost profits.";

        assertSingleQuote(clause);
    }

    @Test
    void separateLiabilitySentencesRemainSeparateFindings() {
        String first =
                "Neither party shall be liable for indirect losses.";
        String second =
                "Total liability shall not exceed the fees paid.";

        SourceDocument document = new SourceDocument(
                "agreement", "1", first + " " + second
        );

        List<RiskFinding> findings = LiabilitySignalFinder.find(
                document, "agreement"
        );

        assertEquals(2, findings.size());
        assertEquals(
                first,
                findings.get(0).evidence().get(0).quote()
        );
        assertEquals(
                second,
                findings.get(1).evidence().get(0).quote()
        );

        findings.forEach(finding ->
                EvidenceValidator.validate(
                        document,
                        finding.evidence().get(0)
                )
        );
    }

    @Test
    void headingAloneDoesNotCreateOperativeLiabilityFinding() {
        SourceDocument document = new SourceDocument(
                "agreement", "1", "Limitation of Liability."
        );

        assertTrue(LiabilitySignalFinder.find(
                document, "agreement").isEmpty());
    }
    @Test
    void capAndExceptionInOneSentenceRemainSeparateReviewCues() {
        String clause = "Total liability shall not exceed the fees paid, "
                + "but this limit will not limit the liability of a "
                + "Party for intentional misconduct.";

        SourceDocument document = new SourceDocument(
                "agreement", "1", clause
        );

        List<RiskFinding> findings = LiabilitySignalFinder.find(
                document, "agreement"
        );

        assertEquals(2, findings.size());
        assertEquals(
                "LIABILITY_CAP_CUE_V1",
                findings.get(0).ruleId()
        );
        assertEquals(
                "LIABILITY_EXCEPTION_CUE_V1",
                findings.get(1).ruleId()
        );

        for (RiskFinding finding : findings) {
            assertEquals(
                    RiskFinding.Priority.REVIEW_ONLY,
                    finding.priority()
            );

            EvidenceSpan span = finding.evidence().get(0);
            EvidenceValidator.validate(document, span);
            assertEquals(clause, span.quote());
        }
    }
    private static void assertSingleQuote(String clause) {
        SourceDocument document =
                new SourceDocument("agreement", "1", clause);

        List<RiskFinding> findings = LiabilitySignalFinder.find(
                document, "agreement"
        );

        assertEquals(1, findings.size());

        EvidenceSpan span = findings.get(0).evidence().get(0);
        EvidenceValidator.validate(document, span);
        assertEquals(clause, span.quote());
    }
}