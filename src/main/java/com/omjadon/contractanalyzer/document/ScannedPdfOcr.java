package com.omjadon.contractanalyzer.document;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public final class ScannedPdfOcr {
    private static final int DPI = 150;
    static final int MAX_OCR_PAGES = 20;
    private static final int MAX_IMAGE_EDGE = 3_000;
    private static final int MAX_PAGE_OUTPUT_BYTES = 400_000;
    private static final int MAX_TOTAL_CHARS = 1_000_000;
    private static final long PAGE_TIMEOUT_SECONDS = 45;

    public String extract(PDDocument pdf) throws IOException {
        Objects.requireNonNull(pdf, "pdf");

        int pages = pdf.getNumberOfPages();
        if (pages < 1 || pages > MAX_OCR_PAGES) {
            throw new IOException(
                    "Scanned PDF OCR supports 1 to "
                            + MAX_OCR_PAGES + " pages"
            );
        }

        StringBuilder result = new StringBuilder();

        for (int pageIndex = 0; pageIndex < pages; pageIndex++) {
            String pageText = extractPage(pdf, pageIndex);

            if (result.length() + pageText.length() + 1
                    > MAX_TOTAL_CHARS) {
                throw new IOException("OCR text exceeds 1 MB limit");
            }

            result.append(pageText).append('\f');
        }

        if (result.toString().replace("\f", "").isBlank()) {
            throw new IOException(
                    "OCR found no readable text in scanned PDF"
            );
        }

        return result.toString();
    }

    public String extractPage(PDDocument pdf, int pageIndex)
            throws IOException {
        Objects.requireNonNull(pdf, "pdf");

        if (pageIndex < 0 || pageIndex >= pdf.getNumberOfPages()) {
            throw new IllegalArgumentException(
                    "Invalid PDF page index for OCR"
            );
        }

        validatePageSize(pdf.getPage(pageIndex).getCropBox());

        PDFRenderer renderer = new PDFRenderer(pdf);
        BufferedImage image = renderer.renderImageWithDPI(
                pageIndex, DPI, ImageType.RGB
        );

        try {
            return recognize(image).strip().replace('\f', ' ');
        } finally {
            image.flush();
        }
    }

    private static void validatePageSize(PDRectangle cropBox)
            throws IOException {
        float widthPixels = cropBox.getWidth() * DPI / 72f;
        float heightPixels = cropBox.getHeight() * DPI / 72f;

        if (!Float.isFinite(widthPixels)
                || !Float.isFinite(heightPixels)
                || widthPixels <= 0
                || heightPixels <= 0
                || widthPixels > MAX_IMAGE_EDGE
                || heightPixels > MAX_IMAGE_EDGE) {
            throw new IOException(
                    "Scanned PDF page exceeds OCR image-size limit"
            );
        }
    }

    private static String recognize(BufferedImage image)
            throws IOException {
        Path input = Files.createTempFile("contract-ocr-", ".png");
        Path output = Files.createTempFile("contract-ocr-", ".txt");
        Path errors = Files.createTempFile("contract-ocr-", ".log");

        try {
            if (!ImageIO.write(image, "png", input.toFile())) {
                throw new IOException(
                        "Unable to render PDF page for OCR"
                );
            }

            Process process = new ProcessBuilder(
                    tesseractExecutable(),
                    input.toString(),
                    "stdout",
                    "-l",
                    "eng"
            )
                    .redirectOutput(output.toFile())
                    .redirectError(errors.toFile())
                    .start();

            boolean completed;
            try {
                completed = process.waitFor(
                        PAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS
                );
            } catch (InterruptedException interrupted) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new IOException(
                        "PDF OCR was interrupted", interrupted
                );
            }

            if (!completed) {
                process.destroyForcibly();
                throw new IOException("PDF OCR page timed out");
            }
            if (process.exitValue() != 0) {
                throw new IOException(
                        "Tesseract OCR failed; verify its English "
                                + "language data is installed"
                );
            }
            if (Files.size(output) > MAX_PAGE_OUTPUT_BYTES) {
                throw new IOException(
                        "OCR page text exceeds output limit"
                );
            }

            return Files.readString(output, StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
            Files.deleteIfExists(errors);
        }
    }

    private static String tesseractExecutable() {
        String configured = System.getenv("TESSERACT_PATH");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }

        if (System.getProperty("os.name")
                .toLowerCase(Locale.ROOT).contains("win")) {
            Path installed = Path.of(
                    "C:\\Program Files\\Tesseract-OCR\\tesseract.exe"
            );
            if (Files.isRegularFile(installed)) {
                return installed.toString();
            }
        }

        return "tesseract";
    }
}