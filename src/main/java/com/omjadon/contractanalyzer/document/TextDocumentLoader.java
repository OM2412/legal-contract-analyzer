package com.omjadon.contractanalyzer.document;

import com.omjadon.contractanalyzer.model.SourceDocument;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public final class TextDocumentLoader {

    private static final long MAX_BYTES = 1_048_576; // 1 MiB

    private TextDocumentLoader() {
    }

    public static SourceDocument load(
            Path path,
            String documentId,
            String version
    ) throws IOException {
        Objects.requireNonNull(path, "path");

        String filename = path.getFileName().toString();

        if (!filename.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            throw new IllegalArgumentException(
                    "Only .txt files are supported at this stage"
            );
        }

        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("File is missing or is not a regular file");
        }

        if (Files.size(path) > MAX_BYTES) {
            throw new IOException("Text file exceeds the 1 MiB limit");
        }

        byte[] bytes = Files.readAllBytes(path);

        // Check again because a file may change between size check and read.
        if (bytes.length > MAX_BYTES) {
            throw new IOException("Text file exceeds the 1 MiB limit");
        }

        final String text;

        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new IOException(
                    "File is not valid UTF-8 text",
                    exception
            );
        }

        if (text.isBlank() || text.indexOf('\0') >= 0) {
            throw new IOException(
                    "File is empty or contains unsupported null characters"
            );
        }

        return new SourceDocument(documentId, version, text);
    }
}