package com.omjadon.contractanalyzer;

import com.omjadon.contractanalyzer.document.PdfDocumentLoader;
import com.omjadon.contractanalyzer.document.PdfPageLocator;
import com.omjadon.contractanalyzer.document.TextDocumentLoader;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;
import com.omjadon.contractanalyzer.payment.PaymentPolicy;
import com.omjadon.contractanalyzer.payment.PaymentPolicyEvaluator;
import com.omjadon.contractanalyzer.payment.PaymentReviewService;
import com.omjadon.contractanalyzer.payment.PaymentTerm;
import com.omjadon.contractanalyzer.sample.SampleDocuments;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ContractAnalyzerApplication {

    private ContractAnalyzerApplication() {
    }

    public static void main(String[] args) {
        if (args.length != 0 && args.length != 2) {
            System.err.println(
                    "Usage: provide no paths for the synthetic demo, "
                            + "or two .txt/.pdf paths: agreement sow"
            );
            return;
        }

        boolean syntheticDemo = args.length == 0;
        boolean agreementPdf = !syntheticDemo && isPdf(args[0]);
        boolean sowPdf = !syntheticDemo && isPdf(args[1]);

        SourceDocument agreement;
        SourceDocument sow;

        try {
            if (syntheticDemo) {
                agreement = SampleDocuments.agreement();
                sow = SampleDocuments.statementOfWork();
            } else {
                agreement = loadDocument(Path.of(args[0]), "agreement-input");
                sow = loadDocument(Path.of(args[1]), "sow-input");
            }
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println(
                    "Unable to load documents: " + exception.getMessage()
            );
            return;
        }

        PaymentPolicy policy = new PaymentPolicy(
                "P-DEMO-30",
                "1.0",
                30,
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
        );

        PaymentReviewService.Review review;

        try {
            review = PaymentReviewService.analyze(agreement, sow, policy);
        } catch (IllegalArgumentException exception) {
            System.err.println(
                    "Unable to analyse documents: " + exception.getMessage()
            );
            return;
        }

        System.out.println(
                syntheticDemo
                        ? "SYNTHETIC PAYMENT REVIEW"
                        : "LOCAL DOCUMENT PAYMENT REVIEW"
        );

        System.out.println("Review status: " + review.status());

        review.comparison().ifPresent(comparison ->
                System.out.println(
                        "Document comparison: " + comparison.status()
                )
        );

        printAssessment(
                "Agreement",
                agreement,
                agreementPdf,
                review.agreementMatches(),
                review.agreementTerm(),
                review.agreementAssessment()
        );

        printAssessment(
                "SOW",
                sow,
                sowPdf,
                review.sowMatches(),
                review.sowTerm(),
                review.sowAssessment()
        );

        System.out.println(
                "Precedence scan: " + review.precedence().status()
        );

        review.precedence().evidence().forEach(evidence -> {
            if (evidence.documentId().equals(agreement.documentId())) {
                printEvidence(
                        "Precedence", agreement, agreementPdf, evidence
                );
            } else if (evidence.documentId().equals(sow.documentId())) {
                printEvidence(
                        "Precedence", sow, sowPdf, evidence
                );
            } else {
                throw new IllegalArgumentException(
                        "Precedence evidence has an unknown document ID"
                );
            }
        });

        review.textualCandidateAssessment().ifPresent(assessment ->
                System.out.println(
                        "Textual candidate assessment: "
                                + assessment.status()
                )
        );

        System.out.println(
                "Policy P-DEMO-30 is an illustrative business preference. "
                        + "Review the complete documents before making a decision."
        );
    }

    private static boolean isPdf(String fileName) {
        return fileName.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private static SourceDocument loadDocument(Path path, String documentId)
            throws IOException {
        String fileName = path.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);
        SourceDocument loaded;

        if (fileName.endsWith(".pdf")) {
            loaded = new PdfDocumentLoader().load(
                    path, documentId, "temporary"
            );
        } else if (fileName.endsWith(".txt")) {
            loaded = TextDocumentLoader.load(
                    path, documentId, "temporary"
            );
        } else {
            throw new IllegalArgumentException(
                    "Only .txt and .pdf files are supported: " + path
            );
        }

        return new SourceDocument(
                documentId,
                textVersion(loaded.text()),
                loaded.text()
        );
    }

    private static String textVersion(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable in this Java installation",
                    exception
            );
        }
    }

    private static void printAssessment(
            String label,
            SourceDocument document,
            boolean pdf,
            List<PaymentTerm> matches,
            Optional<PaymentTerm> term,
            Optional<PaymentPolicyEvaluator.Assessment> assessment
    ) {
        if (matches.size() > 1) {
            System.out.println(
                    label + ": " + matches.size()
                            + " payment clauses recognized; "
                            + "review each obligation"
            );

            for (int index = 0; index < matches.size(); index++) {
                PaymentTerm match = matches.get(index);
                String matchLabel = label + " match " + (index + 1);

                System.out.println(
                        matchLabel + ": "
                                + match.days() + " " + match.dayUnit()
                                + ", trigger=" + match.trigger()
                                + ", payer=" + match.payer()
                                + ", payee=" + match.payee()
                                + ", scope=" + match.scope()
                );

                printEvidence(
                        matchLabel, document, pdf, match.evidence()
                );
            }

            return;
        }

        if (term.isEmpty()) {
            System.out.println(label + ": payment clause not recognized");
            return;
        }

        PaymentTerm candidate = term.orElseThrow();

        System.out.println(
                label + " candidate assessment: "
                        + assessment.orElseThrow().status()
        );
                System.out.println(
                label + " term: days=" + candidate.days()
                        + ", unit=" + candidate.dayUnit()
                        + ", trigger=" + candidate.trigger()
                        + ", payer=" + candidate.payer()
                        + ", payee=" + candidate.payee()
                        + ", scope=" + candidate.scope()
        );

        printEvidence(label, document, pdf, candidate.evidence());
    }

    private static void printEvidence(
            String label,
            SourceDocument document,
            boolean pdf,
            EvidenceSpan evidence
    ) {
        String location;

        if (pdf) {
            PdfPageLocator.PageRange pages =
                    PdfPageLocator.locate(document, evidence);

            location = pages.firstPage() == pages.lastPage()
                    ? "page " + pages.firstPage()
                    : "pages " + pages.firstPage()
                            + "-" + pages.lastPage();
        } else {
            location = "text";
        }

        System.out.println(
                label + " evidence [" + document.documentId()
                        + ", " + location
                        + ", offsets " + evidence.start()
                        + ".." + evidence.end() + "]: "
                        + evidence.quote()
        );
    }
}