package com.omjadon.contractanalyzer.sample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public final class SamplePdfGenerator {

    private SamplePdfGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path folder = Path.of("testdata");
        Files.createDirectories(folder);

        writePdf(
                folder.resolve("agreement.pdf"),
                "Client shall pay Provider the project fee within "
                        + "30 calendar days after receipt of the invoice.",
                "If this Agreement and the SOW differ on payment for the "
                        + "project fee, the SOW payment term prevails."
        );

        writePdf(
                folder.resolve("sow.pdf"),
                "Client shall pay Provider the project fee within "
                        + "60 calendar days after receipt of the invoice."
        );

        System.out.println("Created testdata/agreement.pdf and testdata/sow.pdf");
    }

    private static void writePdf(Path path, String... lines) throws IOException {
        if (Files.exists(path)) {
            throw new IOException("File already exists; refusing to replace: " + path);
        }

        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage();
            pdf.addPage(page);

            try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                stream.beginText();
                stream.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9
                );
                stream.newLineAtOffset(40, 750);

                for (String line : lines) {
                    stream.showText(line);
                    stream.newLineAtOffset(0, -20);
                }

                stream.endText();
            }

            pdf.save(path.toFile());
        }
    }
}