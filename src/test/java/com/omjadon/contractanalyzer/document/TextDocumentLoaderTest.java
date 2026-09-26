package com.omjadon.contractanalyzer.document;

import com.omjadon.contractanalyzer.model.SourceDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextDocumentLoaderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void preservesOriginalLineBreaksAndSpaces() throws IOException {
        Path file = temporaryDirectory.resolve("agreement.txt");
        String original = "Payment  within 30 days.\n\nSecond paragraph.\n";

        Files.writeString(file, original, StandardCharsets.UTF_8);

        SourceDocument document = TextDocumentLoader.load(
                file,
                "agreement-test",
                "1"
        );

        assertEquals(original, document.text());
        assertEquals("agreement-test", document.documentId());
    }

    @Test
    void rejectsInvalidUtf8() throws IOException {
        Path file = temporaryDirectory.resolve("invalid.txt");

        Files.write(file, new byte[] {
                (byte) 0xC3,
                (byte) 0x28
        });

        assertThrows(
                IOException.class,
                () -> TextDocumentLoader.load(file, "invalid", "1")
        );
    }

    @Test
    void rejectsUnsupportedExtension() throws IOException {
        Path file = temporaryDirectory.resolve("agreement.pdf");
        Files.writeString(file, "Sample content", StandardCharsets.UTF_8);

        assertThrows(
                IllegalArgumentException.class,
                () -> TextDocumentLoader.load(file, "agreement", "1")
        );
    }

    @Test
    void rejectsFileAboveSizeLimit() throws IOException {
        Path file = temporaryDirectory.resolve("large.txt");
        Files.write(file, new byte[1_048_577]);

        assertThrows(
                IOException.class,
                () -> TextDocumentLoader.load(file, "large", "1")
        );
    }
}