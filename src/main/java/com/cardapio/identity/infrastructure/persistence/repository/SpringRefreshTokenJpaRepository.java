package com.cardapio.identity.infrastructure.persistence.repository;

import com.cardapio.identity.infrastructure.persistence.jpa.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringRefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {
    Optional<RefreshTokenJpaEntity> findByHashedToken(String hashedToken);

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revoked = true WHERE t.subject = :subject AND t.revoked = false")
    void revokeAllForSubject(@Param("subject") UUID subject);
}
