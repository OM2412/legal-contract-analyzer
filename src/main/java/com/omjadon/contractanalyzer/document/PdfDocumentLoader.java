package com.omjadon.contractanalyzer.document;

import com.omjadon.contractanalyzer.model.SourceDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public final class PdfDocumentLoader {
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_PAGES = 100;
    private static final int MAX_TEXT_CHARS = 1_000_000;

    public SourceDocument load(Path path, String documentId, String version)
            throws IOException {
        Objects.requireNonNull(path, "path");

        if (!path.getFileName().toString()
                .toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException(
                    "Expected a .pdf file: " + path
            );
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "PDF file does not exist or is not a regular file: "
                            + path
            );
        }
        if (Files.size(path) > MAX_FILE_BYTES) {
            throw new IOException("PDF exceeds 10 MB limit: " + path);
        }

        try (PDDocument pdf = Loader.loadPDF(path.toFile())) {
            int pageCount = pdf.getNumberOfPages();

            if (pageCount == 0 || pageCount > MAX_PAGES) {
                throw new IOException(
                        "PDF must contain between 1 and 100 pages: "
                                + path
                );
            }
            if (!pdf.getCurrentAccessPermission().canExtractContent()) {
                throw new IOException(
                        "PDF does not permit text extraction: " + path
                );
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setPageEnd("\f");

            String extracted = stripper.getText(pdf);
            if (extracted.replace("\f", "").isBlank()) {
    String scannedText = new ScannedPdfOcr().extract(pdf);
    return new SourceDocument(documentId, version, scannedText);
}
            String[] pageTexts = extracted.split("\f", -1);

            if (pageTexts.length != pageCount + 1
                    || !pageTexts[pageCount].isEmpty()) {
                throw new IOException(
                        "PDF page boundaries could not be mapped safely"
                );
            }

            ScannedPdfOcr ocr = new ScannedPdfOcr();
            int ocrPages = 0;
            StringBuilder combined = new StringBuilder();

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                String pageText = pageTexts[pageIndex];

                if (pageText.isBlank()) {
                    ocrPages++;
                    if (ocrPages > ScannedPdfOcr.MAX_OCR_PAGES) {
                        throw new IOException(
                                "PDF has more than 20 pages requiring OCR"
                        );
                    }
                    pageText = ocr.extractPage(pdf, pageIndex);
                }

                if (combined.length() + pageText.length() + 1
                        > MAX_TEXT_CHARS) {
                    throw new IOException(
                            "Extracted PDF text exceeds limit: " + path
                    );
                }

                combined.append(pageText).append('\f');
            }

            String text = combined.toString();
            if (text.replace("\f", "").isBlank()) {
                throw new IOException(
                        "OCR found no readable text in scanned PDF"
                );
            }

            return new SourceDocument(documentId, version, text);
        }
    }
}