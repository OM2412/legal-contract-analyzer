package com.omjadon.contractanalyzer.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class UploadErrorPrivacyTest {
    @Test
    void parsingFailuresNeverReturnPathsOrEmbeddedDocumentText() {
        ReviewController controller = new ReviewController(null, null);
        for (Exception error : new Exception[] {
                new IOException("Cannot parse /tmp/contract-review-private.pdf"),
                new IOException("Confidential customer excerpt: private-secret"),
                new IllegalArgumentException("InternalParser private-secret")
        }) {
            var response = controller.invalidUpload(error);
            assertEquals(400, response.getStatusCode().value());
            assertEquals("no-store", response.getHeaders().getCacheControl());
            assertNotNull(response.getBody());
            String body = response.getBody().toString();
            assertFalse(body.contains(error.getMessage()));
            assertFalse(body.contains("private-secret"));
            assertFalse(body.contains("/tmp/"));
        }
    }
}
