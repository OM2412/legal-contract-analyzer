package com.omjadon.contractanalyzer.account;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public final class CredentialVersionFilter
        extends OncePerRequestFilter {

    private final AppUserRepository users;

    public CredentialVersionFilter(AppUserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean current = false;

        if (authentication.getPrincipal()
                instanceof AccountPrincipal principal) {
            current = users.findById(principal.userId())
                    .map(user ->
                            user.credentialVersion()
                                    == principal.credentialVersion()
                    )
                    .orElse(false);
        }

        if (!current) {
            HttpSession session = request.getSession(false);

            if (session != null) {
                session.invalidate();
            }

            SecurityContextHolder.clearContext();

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.getWriter().write(
                    "{\"error\":\"Session expired. Please sign in again.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}