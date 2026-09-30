package com.omjadon.contractanalyzer.web;

import com.omjadon.contractanalyzer.ai.AiPaymentDetailVerifier;
import com.omjadon.contractanalyzer.ai.AiQuoteVerifier;
import com.omjadon.contractanalyzer.ai.LocalOllamaClient;
import com.omjadon.contractanalyzer.document.PdfDocumentLoader;
import com.omjadon.contractanalyzer.document.PdfPageLocator;
import com.omjadon.contractanalyzer.document.TextDocumentLoader;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;
import com.omjadon.contractanalyzer.payment.PaymentPolicy;
import com.omjadon.contractanalyzer.payment.PaymentReviewService;
import com.omjadon.contractanalyzer.payment.PaymentTerm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ReviewController {
    private final LocalOllamaClient ollamaClient;

    public ReviewController(LocalOllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    @PostMapping(
            path = "/api/review",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> review(
            @RequestPart("agreement") MultipartFile agreementFile,
            @RequestPart("sow") MultipartFile sowFile,
            @RequestParam(name = "maxDays", defaultValue = "30") int maxDays
    ) throws IOException {
        if (maxDays < 1 || maxDays > 3650) {
            throw new IllegalArgumentException(
                    "Policy limit must be between 1 and 3650 days"
            );
        }

        SourceDocument agreement =
                readUpload(agreementFile, "agreement-upload");
        SourceDocument sow =
                readUpload(sowFile, "sow-upload");

        PaymentPolicy policy = new PaymentPolicy(
                "P-DEMO-" + maxDays,
                "1.0",
                maxDays,
                PaymentTerm.PaymentTrigger.INVOICE_RECEIPT
        );

        PaymentReviewService.Review result =
                PaymentReviewService.analyze(agreement, sow, policy);

        boolean agreementPdf =
                isPdf(agreementFile.getOriginalFilename());
        boolean sowPdf =
                isPdf(sowFile.getOriginalFilename());

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("reviewStatus", result.status().name());
        response.put("reviewReason", result.reviewReason());
        response.put("agreementVersion", agreement.version());
        response.put("sowVersion", sow.version());
        response.put(
                "agreementOcrPages",
                agreement.ocrPages().stream().sorted().toList()
        );
        response.put(
                "sowOcrPages",
                sow.ocrPages().stream().sorted().toList()
        );

        response.put(
                "comparisonStatus",
                result.comparison()
                        .map(comparison -> comparison.status().name())
                        .orElse(null)
        );
        response.put(
                "comparisonExplanation",
                result.comparison()
                        .map(comparison -> comparison.explanation())
                        .orElse(null)
        );

        response.put(
                "agreementAssessment",
                result.agreementAssessment()
                        .map(assessment -> assessment.status().name())
                        .orElse(null)
        );
        response.put(
                "sowAssessment",
                result.sowAssessment()
                        .map(assessment -> assessment.status().name())
                        .orElse(null)
        );
        response.put(
                "agreementExplanation",
                result.agreementAssessment()
                        .map(assessment -> assessment.explanation())
                        .orElse(null)
        );
        response.put(
                "sowExplanation",
                result.sowAssessment()
                        .map(assessment -> assessment.explanation())
                        .orElse(null)
        );

        response.put(
                "precedenceStatus",
                result.precedence().status().name()
        );
        response.put(
                "precedenceEvidence",
                result.precedence().evidence().stream()
                        .map(span -> {
                            if (span.documentId().equals(
                                    agreement.documentId())) {
                                return evidence(
                                        agreement, span, agreementPdf
                                );
                            }
                            if (span.documentId().equals(
                                    sow.documentId())) {
                                return evidence(sow, span, sowPdf);
                            }
                            throw new IllegalArgumentException(
                                    "Precedence evidence has an unknown "
                                            + "document ID"
                            );
                        })
                        .toList()
        );

        response.put(
                "textualCandidateAssessment",
                result.textualCandidateAssessment()
                        .map(assessment -> assessment.status().name())
                        .orElse(null)
        );
        response.put(
                "textualCandidateExplanation",
                result.textualCandidateAssessment()
                        .map(assessment -> assessment.explanation())
                        .orElse(null)
        );

        response.put(
                "agreementEvidence",
                result.agreementTerm()
                        .map(term -> evidence(
                                agreement,
                                term.evidence(),
                                agreementPdf
                        ))
                        .orElse(null)
        );
        response.put(
                "sowEvidence",
                result.sowTerm()
                        .map(term -> evidence(
                                sow,
                                term.evidence(),
                                sowPdf
                        ))
                        .orElse(null)
        );
                response.put(
                "agreementTerm",
                result.agreementTerm()
                        .map(term -> termDetails(
                                agreement, term, agreementPdf
                        ))
                        .orElse(null)
        );
        response.put(
                "sowTerm",
                result.sowTerm()
                        .map(term -> termDetails(
                                sow, term, sowPdf
                        ))
                        .orElse(null)
        );

        response.put(
                "agreementMatches",
                result.agreementMatches().stream()
                        .map(term -> evidence(
                                agreement,
                                term.evidence(),
                                agreementPdf
                        ))
                        .toList()
        );
        response.put(
                "sowMatches",
                result.sowMatches().stream()
                        .map(term -> evidence(
                                sow,
                                term.evidence(),
                                sowPdf
                        ))
                        .toList()
        );

        response.put(
                "agreementUnrecognized",
                result.agreementUnrecognized().stream()
                        .map(span -> evidence(
                                agreement, span, agreementPdf
                        ))
                        .toList()
        );
        response.put(
                "sowUnrecognized",
                result.sowUnrecognized().stream()
                        .map(span -> evidence(sow, span, sowPdf))
                        .toList()
        );

        response.put("policyMaxDays", maxDays);
        response.put(
                "notice",
                "P-DEMO-" + maxDays
                        + " is an illustrative business preference. "
                        + "Review the complete documents before a decision."
        );

        return response;
    }

    @PostMapping(
            path = "/api/ai/quotes",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> aiQuotes(
            @RequestPart("document") MultipartFile file
    ) throws IOException {
        SourceDocument document = readUpload(file, "ai-input");

        String modelJson = ollamaClient.suggestPaymentQuotes(document);
        var verifiedQuotes = AiQuoteVerifier.locateExactQuotes(
                document, modelJson
        );

        var candidates = verifiedQuotes.stream()
                .limit(2)
                .flatMap(span -> AiPaymentDetailVerifier.verify(
                        ollamaClient.suggestPaymentDetails(span.quote()),
                        span
                ).stream())
                .map(candidate -> {
                    Map<String, Object> details = new LinkedHashMap<>();
                    details.put("days", candidate.days());
                    details.put("dayUnit", candidate.dayUnit());
                    details.put(
                            "invoiceReceiptConfirmed",
                            candidate.invoiceReceiptConfirmed()
                    );
                    details.put(
                            "evidence",
                            evidence(
                                    document,
                                    candidate.evidence(),
                                    isPdf(file.getOriginalFilename())
                            )
                    );
                    return details;
                })
                .toList();

        return Map.of(
                "status", "SUGGESTIONS_ONLY",
                "ocrPages", document.ocrPages().stream()
                        .sorted().toList(),
                "evidence", verifiedQuotes.stream()
                        .map(span -> evidence(
                                document,
                                span,
                                isPdf(file.getOriginalFilename())
                        ))
                        .toList(),
                "candidates", candidates
        );
    }

    @ExceptionHandler({IOException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> invalidUpload(
            Exception error
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", error.getMessage()));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> ollamaUnavailable(
            RestClientException error
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error",
                        "Local Ollama could not complete the request"
                ));
    }

    private static SourceDocument readUpload(
            MultipartFile file,
            String documentId
    ) throws IOException {
        String name = file.getOriginalFilename();
        String suffix;

        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                    documentId + " is empty"
            );
        }

        if (isPdf(name)) {
            suffix = ".pdf";
            if (file.getSize() > 10L * 1024 * 1024) {
                throw new IllegalArgumentException(
                        "PDF exceeds 10 MB limit"
                );
            }
        } else if (name != null
                && name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            suffix = ".txt";
            if (file.getSize() > 1024 * 1024) {
                throw new IllegalArgumentException(
                        "Text file exceeds 1 MB limit"
                );
            }
        } else {
            throw new IllegalArgumentException(
                    "Only .txt and .pdf uploads are supported"
            );
        }

        Path temporary = Files.createTempFile(
                "contract-review-", suffix
        );

        try {
            try (InputStream input = file.getInputStream()) {
                Files.copy(
                        input,
                        temporary,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            SourceDocument loaded = suffix.equals(".pdf")
                    ? new PdfDocumentLoader().load(
                            temporary, documentId, "temporary"
                    )
                    : TextDocumentLoader.load(
                            temporary, documentId, "temporary"
                    );

            return new SourceDocument(
                    documentId,
                    textVersion(loaded.text()),
                    loaded.text(),
                    loaded.ocrPages()
            );
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Map<String, Object> evidence(
            SourceDocument document,
            EvidenceSpan span,
            boolean pdf
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("documentId", span.documentId());
        details.put("documentVersion", span.documentVersion());
        details.put("start", span.start());
        details.put("end", span.end());
        details.put("quote", span.quote());

        if (pdf) {
            PdfPageLocator.PageRange pages =
                    PdfPageLocator.locate(document, span);
            details.put("firstPage", pages.firstPage());
            details.put("lastPage", pages.lastPage());

            boolean anyOcr = false;
            boolean allOcr = true;

            for (int page = pages.firstPage();
                    page <= pages.lastPage(); page++) {
                boolean usedOcr = document.ocrPages().contains(page);
                anyOcr |= usedOcr;
                allOcr &= usedOcr;
            }

            details.put(
                    "textSource",
                    !anyOcr
                            ? "PDF_TEXT"
                            : allOcr
                                    ? "OCR_TEXT"
                                    : "MIXED_OCR_AND_PDF_TEXT"
            );
        } else {
            details.put("textSource", "TXT");
        }

        return details;
    }
        private static Map<String, Object> termDetails(
            SourceDocument document,
            PaymentTerm term,
            boolean pdf
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("days", term.days());
        details.put("dayUnit", term.dayUnit().name());
        details.put("trigger", term.trigger().name());
        details.put("scope", term.scope());
        details.put("payer", term.payer());
        details.put("payee", term.payee());
        details.put(
                "evidence",
                evidence(document, term.evidence(), pdf)
        );
        return details;
    }

    private static boolean isPdf(String name) {
        return name != null
                && name.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private static String textVersion(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(
                    digest.digest(text.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(
                    "SHA-256 unavailable", error
            );
        }
    }
}