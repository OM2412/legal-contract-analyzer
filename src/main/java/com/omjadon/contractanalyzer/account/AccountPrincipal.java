package com.omjadon.contractanalyzer.account;

import java.util.UUID;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

public final class AccountPrincipal extends User {

    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final long credentialVersion;

    public AccountPrincipal(AppUser user) {
        super(
                user.email(),
                user.passwordHash(),
                AuthorityUtils.createAuthorityList("ROLE_USER")
        );

        this.userId = user.id();
        this.credentialVersion = user.credentialVersion();
    }

    public UUID userId() {
        return userId;
    }

    public long credentialVersion() {
        return credentialVersion;
    }
}