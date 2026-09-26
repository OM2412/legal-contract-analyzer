package com.omjadon.contractanalyzer.document;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.omjadon.contractanalyzer.evidence.EvidenceValidator;
import com.omjadon.contractanalyzer.model.EvidenceSpan;
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

class PdfPageLocatorTest {
    @TempDir
    Path tempDir;

    @Test
    void locatesEvidenceOnSecondPdfPage() throws IOException {
        Path file = tempDir.resolve("two-pages.pdf");

        try (PDDocument pdf = new PDDocument()) {
            addPage(pdf, "Agreement overview");
            addPage(pdf, "Payment is due within 30 calendar days.");
            pdf.save(file.toFile());
        }

        SourceDocument document =
                new PdfDocumentLoader().load(file, "agreement", "v1");

        String quote = "Payment is due within 30 calendar days.";
        int startIndex = document.text().indexOf(quote);
        int endIndex = startIndex + quote.length();

        int start = document.text().codePointCount(0, startIndex);
        int end = document.text().codePointCount(0, endIndex);

        EvidenceSpan evidence = EvidenceValidator.fromRange(
                document, start, end
        );

        assertEquals(quote, evidence.quote());
        assertEquals(
                new PdfPageLocator.PageRange(2, 2),
                PdfPageLocator.locate(document, evidence)
        );
    }

    private static void addPage(PDDocument pdf, String text)
            throws IOException {
        PDPage page = new PDPage();
        pdf.addPage(page);

        try (PDPageContentStream stream =
                     new PDPageContentStream(pdf, page)) {
            stream.beginText();
            stream.setFont(
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                    10
            );
            stream.newLineAtOffset(40, 700);
            stream.showText(text);
            stream.endText();
        }
    }
}