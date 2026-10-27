package com.omjadon.contractanalyzer.account;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PasswordResetMailSender {

    private static final URI RESEND_ENDPOINT =
            URI.create("https://api.resend.com/emails");

    private static final JsonMapper JSON =
            JsonMapper.builder().build();

    private static final HttpClient HTTP =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();

    private final ObjectProvider<JavaMailSender> mailSenders;
    private final boolean enabled;
    private final String baseUrl;
    private final String fromAddress;
    private final String provider;
    private final String resendApiKey;

    @Autowired
    public PasswordResetMailSender(
            ObjectProvider<JavaMailSender> mailSenders,
            @Value("${folio.password-reset.enabled:false}")
            boolean enabled,
            @Value("${folio.password-reset.base-url:http://127.0.0.1:5173}")
            String baseUrl,
            @Value("${folio.password-reset.from:}")
            String fromAddress,
            @Value("${folio.password-reset.provider:smtp}")
            String provider,
            @Value("${folio.password-reset.resend-api-key:}")
            String resendApiKey
    ) {
        this.mailSenders = mailSenders;
        this.enabled = enabled;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.fromAddress = fromAddress.trim();
        this.provider = provider.trim()
                .toLowerCase(Locale.ROOT);
        this.resendApiKey = resendApiKey.trim();

        if (enabled) {
            validateConfiguration();
        }
    }

    /**
     * Preserves the SMTP constructor used by existing tests.
     */
    public PasswordResetMailSender(
            ObjectProvider<JavaMailSender> mailSenders,
            boolean enabled,
            String baseUrl,
            String fromAddress
    ) {
        this(
                mailSenders,
                enabled,
                baseUrl,
                fromAddress,
                "smtp",
                ""
        );
    }

    public boolean isAvailable() {
        if (!enabled) {
            return false;
        }

        if ("resend".equals(provider)) {
            return !resendApiKey.isBlank();
        }

        return "smtp".equals(provider)
                && mailSenders.getIfAvailable() != null;
    }

    public void sendResetLink(
            String email,
            String rawToken,
            Instant expiresAt
    ) {
        if (!isAvailable()) {
            throw new IllegalStateException(
                    "Password recovery is currently unavailable"
            );
        }

        if (rawToken == null
                || !rawToken.matches("[A-Za-z0-9_-]{43}")) {
            throw new IllegalArgumentException(
                    "Invalid reset token format"
            );
        }

        String link = baseUrl
                + "/#reset-password="
                + rawToken;

        String subject = "Reset your Folio password";

        String text =
                "A password reset was requested for your Folio account.\n\n"
                        + "Open this link to choose a new password:\n"
                        + link
                        + "\n\nThis link expires at "
                        + expiresAt
                        + " (UTC) and can be used only once.\n\n"
                        + "If you did not request this reset, "
                        + "you can ignore this email.\n"
                        + "Your password has not been changed.";

        if ("resend".equals(provider)) {
            sendViaResend(email, subject, text);
        } else {
            sendViaSmtp(email, subject, text);
        }
    }

    private void sendViaSmtp(
            String email,
            String subject,
            String text
    ) {
        JavaMailSender sender = mailSenders.getIfAvailable();

        if (sender == null) {
            throw deliveryFailure();
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(text);

        try {
            sender.send(message);
        } catch (MailException error) {
            throw deliveryFailure();
        }
    }

    private void sendViaResend(
            String email,
            String subject,
            String text
    ) {
        String payload = JSON.writeValueAsString(Map.of(
                "from", fromAddress,
                "to", List.of(email),
                "subject", subject,
                "text", text
        ));

        HttpRequest request = HttpRequest.newBuilder(
                        RESEND_ENDPOINT
                )
                .timeout(Duration.ofSeconds(10))
                .header(
                        "Authorization",
                        "Bearer " + resendApiKey
                )
                .header(
                        "Content-Type",
                        "application/json"
                )
                .header("Accept", "application/json")
                .header("User-Agent", "Folio/0.1")
                .POST(HttpRequest.BodyPublishers.ofString(
                        payload,
                        StandardCharsets.UTF_8
                ))
                .build();

        try {
            HttpResponse<Void> response = HTTP.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            if (response.statusCode() != 200) {
                throw deliveryFailure();
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw deliveryFailure();
        } catch (IOException error) {
            throw deliveryFailure();
        }
    }

    private static IllegalStateException deliveryFailure() {
        return new IllegalStateException(
                "Password recovery email could not be sent"
        );
    }

    private void validateConfiguration() {
        URI uri = URI.create(baseUrl);

        String host = uri.getHost();

        boolean local = "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host);

        boolean allowedScheme =
                "https".equalsIgnoreCase(uri.getScheme())
                        || (
                            local
                            && "http".equalsIgnoreCase(
                                    uri.getScheme()
                            )
                        );

        if (host == null
                || !allowedScheme
                || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || !(uri.getPath().isEmpty()
                        || "/".equals(uri.getPath()))) {
            throw new IllegalArgumentException(
                    "Password recovery requires a valid application root URL"
            );
        }

        if (fromAddress.isBlank()
                || fromAddress.contains("\r")
                || fromAddress.contains("\n")
                || !fromAddress.contains("@")) {
            throw new IllegalArgumentException(
                    "Password recovery requires a sender email address"
            );
        }

        if (!"smtp".equals(provider)
                && !"resend".equals(provider)) {
            throw new IllegalArgumentException(
                    "Password recovery provider must be smtp or resend"
            );
        }

        if ("resend".equals(provider)
                && (
                    resendApiKey.isBlank()
                    || resendApiKey.chars().anyMatch(
                            character ->
                                    character < 33
                                            || character > 126
                    )
                )) {
            throw new IllegalArgumentException(
                    "Password recovery requires a valid Resend API key"
            );
        }
    }
}