package com.omjadon.contractanalyzer.account;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from PasswordResetToken token
            where token.tokenHash = :hash
            """)
    Optional<PasswordResetToken> findByHashForUpdate(
            @Param("hash") String hash
    );

    @Modifying
    @Query("""
            delete from PasswordResetToken token
            where token.userId = :userId
            """)
    int deleteAllForUser(@Param("userId") UUID userId);
}