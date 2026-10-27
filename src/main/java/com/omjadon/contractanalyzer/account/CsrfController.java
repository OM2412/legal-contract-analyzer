package com.omjadon.contractanalyzer.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {

    @GetMapping("/api/auth/csrf")
    public void csrf(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        CsrfToken token = (CsrfToken) request.getAttribute(
                CsrfToken.class.getName()
        );

        if (token == null) {
            throw new IllegalStateException(
                    "CSRF token is unavailable"
            );
        }

        // Force token creation so Spring Security sends the XSRF-TOKEN cookie.
        token.getToken();
        response.setHeader("Cache-Control", "no-store");
    }
}