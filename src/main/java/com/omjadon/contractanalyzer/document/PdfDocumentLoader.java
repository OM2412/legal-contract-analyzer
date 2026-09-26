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

        if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Expected a .pdf file: " + path);
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("PDF file does not exist or is not a regular file: " + path);
        }
        if (Files.size(path) > MAX_FILE_BYTES) {
            throw new IOException("PDF exceeds 10 MB limit: " + path);
        }

        try (PDDocument pdf = Loader.loadPDF(path.toFile())) {
            if (pdf.getNumberOfPages() == 0 || pdf.getNumberOfPages() > MAX_PAGES) {
                throw new IOException("PDF must contain between 1 and 100 pages: " + path);
            }
            if (!pdf.getCurrentAccessPermission().canExtractContent()) {
                throw new IOException("PDF does not permit text extraction: " + path);
            }
PDFTextStripper stripper = new PDFTextStripper();
stripper.setSortByPosition(true);
stripper.setPageEnd("\f");
String text = stripper.getText(pdf);

            if (text.isBlank()) {
                throw new IOException(
                        "No readable text found in PDF; scanned PDFs need OCR: " + path);
            }
            if (text.length() > MAX_TEXT_CHARS) {
                throw new IOException("Extracted PDF text exceeds limit: " + path);
            }

            return new SourceDocument(documentId, version, text);
        }
    }
}