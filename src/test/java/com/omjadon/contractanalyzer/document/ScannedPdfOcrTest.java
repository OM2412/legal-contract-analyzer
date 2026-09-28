package com.omjadon.contractanalyzer.document;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
import com.omjadon.contractanalyzer.model.SourceDocument;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScannedPdfOcrTest {
    @TempDir
    Path tempDir;

    @Test
    void extractsImageOnlyTextAndLocatesSecondPage() throws Exception {
        Assumptions.assumeTrue(
                Files.isRegularFile(Path.of(
                        "C:\\Program Files\\Tesseract-OCR\\tesseract.exe"
                )),
                "Local OCR integration test requires Tesseract"
        );

        Path file = tempDir.resolve("scanned-agreement.pdf");

        try (PDDocument pdf = new PDDocument()) {
            addImagePage(pdf, "SCANNED AGREEMENT", null);
            addImagePage(
                    pdf,
                    "Client shall pay Provider the project fee",
                    "within 30 calendar days after receipt of the invoice."
            );
            pdf.save(file.toFile());
        }

        SourceDocument document =
                new PdfDocumentLoader().load(file, "scanned", "1");

        String quote = "30 calendar days";
        int utf16Start = document.text().indexOf(quote);

        assertTrue(
                utf16Start >= 0,
                "OCR should find the payment deadline on page 2"
        );

        int start = document.text().codePointCount(0, utf16Start);
        int end = start + quote.codePointCount(0, quote.length());

        EvidenceSpan evidence = EvidenceValidator.fromRange(
                document, start, end
        );
        PdfPageLocator.PageRange pages =
                PdfPageLocator.locate(document, evidence);

        assertEquals(quote, evidence.quote());
        assertEquals(2, pages.firstPage());
        assertEquals(2, pages.lastPage());
    }

    private static void addImagePage(
            PDDocument pdf,
            String firstLine,
            String secondLine
    ) throws Exception {
        BufferedImage image = new BufferedImage(
                1400, 350, BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("SansSerif", Font.BOLD, 38));
            graphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
            graphics.drawString(firstLine, 45, 115);
            if (secondLine != null) {
                graphics.drawString(secondLine, 45, 215);
            }
        } finally {
            graphics.dispose();
        }

        PDPage page = new PDPage();
        pdf.addPage(page);
        PDImageXObject embedded =
                LosslessFactory.createFromImage(pdf, image);
        image.flush();

        try (PDPageContentStream stream =
                     new PDPageContentStream(pdf, page)) {
            stream.drawImage(embedded, 40, 485, 530, 170);
        }
    }
}