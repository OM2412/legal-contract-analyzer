package com.omjadon.contractanalyzer.account;

import java.net.URI;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetMailSender {

    private final ObjectProvider<JavaMailSender> mailSenders;
    private final boolean enabled;
    private final String baseUrl;
    private final String fromAddress;

    public PasswordResetMailSender(
            ObjectProvider<JavaMailSender> mailSenders,
            @Value("${folio.password-reset.enabled:false}")
            boolean enabled,
            @Value("${folio.password-reset.base-url:http://127.0.0.1:5173}")
            String baseUrl,
            @Value("${folio.password-reset.from:}")
            String fromAddress
    ) {
        this.mailSenders = mailSenders;
        this.enabled = enabled;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.fromAddress = fromAddress.trim();

        if (enabled) {
            validateConfiguration();
        }
    }

    public boolean isAvailable() {
        return enabled && mailSenders.getIfAvailable() != null;
    }

    public void sendResetLink(
            String email,
            String rawToken,
            Instant expiresAt
    ) {
        JavaMailSender sender = mailSenders.getIfAvailable();

        if (!enabled || sender == null) {
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

        // The fragment will be read by the frontend reset screen.
        String link = baseUrl + "/#reset-password=" + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Reset your Folio password");
        message.setText(
                "A password reset was requested for your Folio account.\n\n"
                        + "Open this link to choose a new password:\n"
                        + link
                        + "\n\nThis link expires at "
                        + expiresAt
                        + " (UTC) and can be used only once.\n\n"
                        + "If you did not request this reset, "
                        + "you can ignore this email.\n"
                        + "Your password has not been changed."
        );

        try {
            sender.send(message);
        } catch (MailException error) {
            throw new IllegalStateException(
                    "Password recovery email could not be sent"
            );
        }
    }

    private void validateConfiguration() {
        URI uri = URI.create(baseUrl);

        String host = uri.getHost();
        boolean local = "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host);

        boolean allowedScheme = "https".equalsIgnoreCase(
                uri.getScheme()
        ) || (local && "http".equalsIgnoreCase(uri.getScheme()));

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
    }
}