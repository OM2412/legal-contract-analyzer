package com.omjadon.contractanalyzer.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerminationSignalExpansionTest {

    @Test
    void namedPartiesProduceOneExactReviewSignal() {
        String clause = "Either Consultant or Company may terminate this "
                + "Agreement upon prior written notice to the other party.";

        assertSingleExactClause(clause, clause);
    }

    @Test
    void passiveWordingProducesOneSignalDespiteOverlappingPatterns() {
        String clause = "This Agreement may be terminated by either party "
                + "upon ninety (90) days written notice.";

        assertSingleExactClause(clause, clause);
    }

    @Test
    void continuationUntilTerminationIsFlagged() {
        String clause = "After the Initial Term, this Agreement shall "
                + "continue on a month to month basis until terminated "
                + "by either party upon thirty (30) days prior notice.";

        assertSingleExactClause(clause, clause);
    }

    @Test
    void existingExplicitPatternKeepsItsOriginalEvidence() {
        String clause =
                "Either party may terminate this Agreement for cause.";
        String text = "Other provisions apply. " + clause;

        assertSingleExactClause(text, clause);
    }

    @Test
    void mayNotTerminateIsNotReadAsMayTerminate() {
        String text = "Either party may not terminate this Agreement "
                + "before the anniversary.";
        SourceDocument document =
                new SourceDocument("agreement", "1", text);

        assertTrue(TerminationSignalFinder.find(
                document, "agreement").isEmpty());
    }

    private static void assertSingleExactClause(
            String text,
            String expectedQuote
    ) {
        SourceDocument document =
                new SourceDocument("agreement", "1", text);

        List<RiskFinding> findings = TerminationSignalFinder.find(
                document, "agreement"
        );

        assertEquals(1, findings.size());
        assertEquals(1, findings.get(0).evidence().size());

        EvidenceSpan span = findings.get(0).evidence().get(0);
        EvidenceValidator.validate(document, span);
        assertEquals(expectedQuote, span.quote());
    }
}