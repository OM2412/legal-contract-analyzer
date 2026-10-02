package com.omjadon.contractanalyzer.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public final class LocalAiAvailabilityFilter
        extends OncePerRequestFilter {

    private final boolean aiEnabled;

    public LocalAiAvailabilityFilter(
            @Value("${folio.ai.enabled:true}") boolean aiEnabled
    ) {
        this.aiEnabled = aiEnabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String aiPath =
                request.getContextPath() + "/api/ai/quotes";

        if (!aiEnabled
                && request.getRequestURI().equals(aiPath)) {
            response.setStatus(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE
            );
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"error\":\"Local AI is unavailable "
                            + "in this deployment.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}