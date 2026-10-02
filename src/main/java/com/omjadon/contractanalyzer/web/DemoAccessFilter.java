package com.omjadon.contractanalyzer.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public final class DemoAccessFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final byte[] expectedCredentials;

    public DemoAccessFilter(
            @Value("${FOLIO_DEMO_AUTH_ENABLED:false}") boolean enabled,
            @Value("${FOLIO_DEMO_USERNAME:}") String username,
            @Value("${FOLIO_DEMO_PASSWORD:}") String password
    ) {
        if (enabled && (username.isBlank() || password.isBlank())) {
            throw new IllegalStateException(
                    "Demo authentication requires both username and password"
            );
        }

        this.enabled = enabled;
        this.expectedCredentials = (username + ":" + password)
                .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        if (!enabled || validCredentials(request.getHeader("Authorization"))) {
            chain.doFilter(request, response);
            return;
        }

        response.setHeader(
                "WWW-Authenticate",
                "Basic realm=\"Folio Demo\", charset=\"UTF-8\""
        );
        response.setHeader("Cache-Control", "no-store");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private boolean validCredentials(String authorization) {
        if (authorization == null
                || authorization.length() > 8192
                || !authorization.regionMatches(
                        true, 0, "Basic ", 0, 6
                )) {
            return false;
        }

        try {
            byte[] supplied = Base64.getDecoder().decode(
                    authorization.substring(6).trim()
            );
            return MessageDigest.isEqual(
                    supplied, expectedCredentials
            );
        } catch (IllegalArgumentException error) {
            return false;
        }
    }
}