package com.omjadon.contractanalyzer.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> uploadTooLarge(
            MaxUploadSizeExceededException error
    ) {
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "error",
                        "Upload exceeds the allowed size. "
                                + "Each file must be at most 10 MiB, "
                                + "and the complete request at most 20 MiB."
                ));
    }
}