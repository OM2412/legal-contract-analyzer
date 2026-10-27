package com.omjadon.contractanalyzer.payment;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.SourceDocument;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentNamedPartyCoverageTest {

    private static final String AGREEMENT_QUOTE =
            "Customer shall pay Atlas Systems the monthly support "
                    + "charge within 24 calendar days after Customer "
                    + "receives Atlas Systems' invoice.";

    private static final String FIRST_SOW_QUOTE =
            "Customer must pay Atlas Systems 35 percent of the "
                    + "migration fee within 12 business days "
                    + "after the invoice date.";

    private static final String SECOND_SOW_QUOTE =
            "Customer must pay Atlas Systems the remaining 65 percent "
                    + "within 8 calendar days after Customer gives "
                    + "final written acceptance of the migration "
                    + "deliverable.";

    private static final PaymentPolicy POLICY = new PaymentPolicy(
            "P-DEMO-30",
            "1.0",
            30,
            PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
    );

    @Test
    void pilotRetainsBothInstallmentsWithoutChoosingOne()
            throws Exception {
        SourceDocument agreement = loadPilot("agreement");
        SourceDocument sow = loadPilot("sow");

        var review = PaymentReviewService.analyze(
                agreement, sow, POLICY
        );

        assertEquals(
                PaymentReviewService.ReviewStatus.REVIEW_REQUIRED,
                review.status()
        );

        assertEquals(1, review.agreementMatches().size());
        assertEquals(2, review.sowMatches().size());
        assertTrue(review.agreementUnrecognized().isEmpty());
        assertTrue(review.sowUnrecognized().isEmpty());

        assertEquals(
                AGREEMENT_QUOTE,
                review.agreementMatches().get(0).evidence().quote()
        );
        assertEquals(
                FIRST_SOW_QUOTE,
                review.sowMatches().get(0).evidence().quote()
        );

        var remaining = review.sowMatches().get(1);

        assertTrue(
                remaining.evidence().quote().startsWith(FIRST_SOW_QUOTE)
        );
        assertTrue(
                remaining.evidence().quote().endsWith(SECOND_SOW_QUOTE)
        );
        assertEquals(
                "remaining 65 percent of the migration fee",
                remaining.scope()
        );

        review.agreementMatches().forEach(term ->
                EvidenceValidator.validate(agreement, term.evidence())
        );
        review.sowMatches().forEach(term ->
                EvidenceValidator.validate(sow, term.evidence())
        );

        assertTrue(review.sowTerm().isEmpty());
        assertTrue(review.comparison().isEmpty());
        assertTrue(review.textualCandidateAssessment().isEmpty());
    }

    @Test
    void invoiceIssuanceAndNegationDoNotCreateCoverageFindings() {
        for (String text : List.of(
                "Atlas Systems must issue its invoice within five "
                        + "days after the kickoff meeting.",
                "Customer shall not pay Atlas Systems the fee.",
                "Customer must pay attention to invoice wording."
        )) {
            SourceDocument document = new SourceDocument(
                    "negative", "1", text
            );

            assertTrue(
                    PaymentClauseCoverageScanner.findUnrecognized(
                            document, List.of()
                    ).isEmpty(),
                    text
            );
        }
    }

    @Test
    void coveragePreservesUnicodeOffsetsAndStopsAtPageBoundary() {
        String clause =
                "Customer must pay Atlas Systems the support charge";

        SourceDocument document = new SourceDocument(
                "unicode",
                "1",
                "\uD83D\uDCC4 " + clause + "\fNext page text."
        );

        var findings =
                PaymentClauseCoverageScanner.findUnrecognized(
                        document, List.of()
                );

        assertEquals(1, findings.size());

        var span = findings.get(0);

        assertEquals(clause, span.quote());
        assertEquals(2, span.start());
        assertEquals(
                2 + clause.codePointCount(0, clause.length()),
                span.end()
        );

        EvidenceValidator.validate(document, span);
    }

    @Test
    void recognizedPaymentDoesNotGetDuplicateCoverageWarning() {
        SourceDocument document = new SourceDocument(
                "recognized",
                "1",
                "Client shall pay Provider the project fee within "
                        + "30 calendar days after receipt of the invoice."
        );

        var terms = PaymentTermExtractor.extractAll(document);

        assertEquals(1, terms.size());

        assertTrue(
                PaymentClauseCoverageScanner.findUnrecognized(
                        document, terms
                ).isEmpty()
        );
    }

    private static SourceDocument loadPilot(String role)
            throws Exception {
        Path path = Path.of(
                "testdata",
                "product-evaluation",
                "heldout",
                "PAIR-001-" + role + ".txt"
        );

        return new SourceDocument(
                role,
                "development-regression",
                Files.readString(path, StandardCharsets.UTF_8)
        );
    }
}