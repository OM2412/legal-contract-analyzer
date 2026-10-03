package com.omjadon.contractanalyzer.risk;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminationSignalFinderTest {

    @Test
    void locatesExplicitClauseWithExactEvidence() {
        String clause =
                "Either party may terminate this Agreement upon "
                        + "30 days' written notice.";
        SourceDocument document = new SourceDocument(
                "agreement", "1", "Introductory text. " + clause
        );

        List<RiskFinding> findings =
                TerminationSignalFinder.find(
                        document, "agreement"
                );

        assertEquals(1, findings.size());

        RiskFinding finding = findings.get(0);

        assertEquals(
                RiskFinding.Category.TERMINATION,
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
        assertEquals(clause, finding.evidence().get(0).quote());
        EvidenceValidator.validate(
                document, finding.evidence().get(0)
        );
    }

    @Test
    void doesNotFlagNegatedOrHistoricalMention() {
        SourceDocument document = new SourceDocument(
                "agreement",
                "1",
                "The Client may not terminate this Agreement. "
                        + "A previous Agreement was terminated."
        );

        assertTrue(
                TerminationSignalFinder.find(
                        document, "agreement"
                ).isEmpty()
        );
    }

    @Test
    void offsetsCountUnicodeCodePoints() {
        String clause =
                "Client may terminate the SOW for convenience.";
        SourceDocument document = new SourceDocument(
                "sow", "1", "🚀 " + clause
        );

        RiskFinding finding =
                TerminationSignalFinder.find(document, "sow")
                        .get(0);

        assertEquals(2, finding.evidence().get(0).start());
        assertEquals(clause, finding.evidence().get(0).quote());
        EvidenceValidator.validate(
                document, finding.evidence().get(0)
        );
    }
}