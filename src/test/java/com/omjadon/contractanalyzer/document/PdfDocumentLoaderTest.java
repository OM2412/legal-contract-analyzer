package com.omjadon.contractanalyzer.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.omjadon.contractanalyzer.model.SourceDocument;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfDocumentLoaderTest {
    @TempDir
    Path tempDir;

    private final PdfDocumentLoader loader = new PdfDocumentLoader();

    @Test
    void readsTextFromPdf() throws IOException {
        Path file = tempDir.resolve("agreement.pdf");
        String clause = "Client shall pay Provider the project fee within "
                + "30 calendar days after receipt of the invoice.";

        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage();
            pdf.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                stream.beginText();
                stream.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                stream.newLineAtOffset(40, 700);
                stream.showText(clause);
                stream.endText();
            }

            pdf.save(file.toFile());
        }

        SourceDocument result = loader.load(file, "agreement", "1");

        assertEquals("agreement", result.documentId());
        assertEquals("1", result.version());
        assertTrue(result.text().contains(clause));
    }

    @Test
    void rejectsPdfWithoutReadableText() throws IOException {
        Path file = tempDir.resolve("blank.pdf");

        try (PDDocument pdf = new PDDocument()) {
            pdf.addPage(new PDPage());
            pdf.save(file.toFile());
        }

        IOException error = assertThrows(IOException.class,
                () -> loader.load(file, "blank", "1"));

        assertTrue(error.getMessage().contains("No readable text"));
    }
}