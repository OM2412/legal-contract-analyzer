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
    void pilotRequiresReviewWithThreeExactCoverageQuotes()
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

        assertEquals(1, review.agreementUnrecognized().size());
        assertEquals(2, review.sowUnrecognized().size());

        assertEquals(
                AGREEMENT_QUOTE,
                review.agreementUnrecognized().get(0).quote()
        );
        assertEquals(
                FIRST_SOW_QUOTE,
                review.sowUnrecognized().get(0).quote()
        );
        assertEquals(
                SECOND_SOW_QUOTE,
                review.sowUnrecognized().get(1).quote()
        );

        review.agreementUnrecognized().forEach(span ->
                EvidenceValidator.validate(agreement, span)
        );
        review.sowUnrecognized().forEach(span ->
                EvidenceValidator.validate(sow, span)
        );

        assertTrue(review.comparison().isEmpty());
        assertTrue(review.textualCandidateAssessment().isEmpty());
    }

    @Test
    void invoiceIssuanceAndNegationDoNotCreateCoverageFindings() {
        List<String> negatives = List.of(
                "Atlas Systems must issue its invoice within five "
                        + "days after the kickoff meeting.",
                "Customer shall not pay Atlas Systems the fee.",
                "Customer must pay attention to invoice wording."
        );

        for (String text : negatives) {
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
                "unicode", "1",
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
                "recognized", "1",
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