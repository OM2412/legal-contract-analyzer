package com.omjadon.contractanalyzer.account;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class PasswordRecoveryController {

    private static final Logger LOG =
            LoggerFactory.getLogger(PasswordRecoveryController.class);

    private static final String ACKNOWLEDGEMENT =
            "If an account exists for this email, "
                    + "a password reset link will be sent.";

    private final PasswordResetService resets;
    private final PasswordResetMailSender mailSender;
    private final PasswordRecoveryRateLimiter limiter;

    public PasswordRecoveryController(
            PasswordResetService resets,
            PasswordResetMailSender mailSender,
            PasswordRecoveryRateLimiter limiter
    ) {
        this.resets = resets;
        this.mailSender = mailSender;
        this.limiter = limiter;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> requestReset(
            @RequestBody ForgotPasswordRequest body,
            HttpServletRequest request
    ) {
        String email = body.email();

        if (!limiter.allowRequest(request.getRemoteAddr(), email)) {
            return rateLimited();
        }

        // Check configuration before looking up an account.
        if (!mailSender.isAvailable()) {
            return response(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "error",
                    "Password recovery is currently unavailable"
            );
        }

        try {
            resets.requestReset(email);
        } catch (IllegalStateException error) {
            // Keep the public acknowledgement unchanged on a
            // delivery failure. Never log the email or reset token.
            LOG.warn("Password recovery delivery failed");
        }

        return response(
                HttpStatus.ACCEPTED,
                "message",
                ACKNOWLEDGEMENT
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody ResetPasswordRequest body,
            HttpServletRequest request
    ) {
        if (!limiter.allowReset(request.getRemoteAddr())) {
            return rateLimited();
        }

        resets.resetPassword(body.token(), body.password());

        return response(
                HttpStatus.OK,
                "message",
                "Password updated. Sign in with your new password."
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidRequest(
            IllegalArgumentException error
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                "error",
                error.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> unavailable(
            IllegalStateException error
    ) {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "error",
                "Password recovery is currently unavailable"
        );
    }

    private static ResponseEntity<Map<String, String>> rateLimited() {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .cacheControl(CacheControl.noStore())
                .header("Retry-After", "900")
                .body(Map.of(
                        "error",
                        "Too many password recovery attempts. "
                                + "Please try again later."
                ));
    }

    private static ResponseEntity<Map<String, String>> response(
            HttpStatus status,
            String key,
            String message
    ) {
        return ResponseEntity
                .status(status)
                .cacheControl(CacheControl.noStore())
                .body(Map.of(key, message));
    }

    public record ForgotPasswordRequest(String email) {
    }

    public record ResetPasswordRequest(
            String token,
            String password
    ) {
    }
}