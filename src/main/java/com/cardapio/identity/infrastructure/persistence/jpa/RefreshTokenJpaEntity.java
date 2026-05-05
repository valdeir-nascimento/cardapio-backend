package com.cardapio.identity.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(nullable = false) private UUID subject;
    @Column(nullable = false, length = 20) private String audience;
    @Column(name = "hashed_token", nullable = false, length = 120, unique = true) private String hashedToken;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(nullable = false) private boolean revoked;

    protected RefreshTokenJpaEntity() {}

    public RefreshTokenJpaEntity(UUID id, UUID subject, String audience, String hashedToken, Instant issuedAt, Instant expiresAt, boolean revoked) {
        this.id = id; this.subject = subject; this.audience = audience; this.hashedToken = hashedToken;
        this.issuedAt = issuedAt; this.expiresAt = expiresAt; this.revoked = revoked;
    }

    public UUID getId() { return id; }
    public UUID getSubject() { return subject; }
    public String getAudience() { return audience; }
    public String getHashedToken() { return hashedToken; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }

    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
