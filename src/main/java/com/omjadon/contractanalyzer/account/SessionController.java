package com.omjadon.contractanalyzer.account;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class SessionController {

    private final AppUserRepository users;

    public SessionController(AppUserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    public Session me(Authentication authentication) {
        AppUser user = users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Account is no longer available"
                ));

        return new Session(
                user.id(),
                user.email(),
                user.displayName()
        );
    }

    public record Session(
            UUID id,
            String email,
            String displayName
    ) {
    }
}