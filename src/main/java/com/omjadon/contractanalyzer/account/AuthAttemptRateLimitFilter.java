package com.omjadon.contractanalyzer.account;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public final class AuthAttemptRateLimitFilter
        extends OncePerRequestFilter {

    private final AuthAttemptRateLimiter limiter;

    public AuthAttemptRateLimitFilter(
            AuthAttemptRateLimiter limiter
    ) {
        this.limiter = limiter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        AuthAttemptRateLimiter.Operation operation = null;

        if ("POST".equals(request.getMethod())) {
            String path = request.getServletPath();

            if ("/api/auth/login".equals(path)) {
                operation = AuthAttemptRateLimiter.Operation.LOGIN;
            } else if ("/api/auth/register".equals(path)) {
                operation = AuthAttemptRateLimiter.Operation.REGISTER;
            }
        }

        if (operation == null) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthAttemptRateLimiter.Decision decision =
                limiter.acquire(
                        operation,
                        request.getRemoteAddr()
                );

        if (!decision.allowed()) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader(
                    "Retry-After",
                    Long.toString(decision.retryAfterSeconds())
            );

            response.getWriter().write(
                    "{\"error\":\"Too many attempts. "
                            + "Please try again later.\"}"
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}