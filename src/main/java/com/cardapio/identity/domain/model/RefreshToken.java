package com.cardapio.identity.domain.model;

import com.cardapio.shared.domain.AggregateRoot;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RefreshToken extends AggregateRoot<RefreshTokenId> {

    private final UUID subject;
    private final Audience audience;
    private final String hashedToken;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private boolean revoked;

    private RefreshToken(RefreshTokenId id, UUID subject, Audience audience,
                         String hashedToken, Instant issuedAt, Instant expiresAt, boolean revoked) {
        super(id);
        this.subject = Objects.requireNonNull(subject, "subject");
        this.audience = Objects.requireNonNull(audience, "audience");
        this.hashedToken = Objects.requireNonNull(hashedToken, "hashedToken");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.revoked = revoked;
    }

    public static RefreshToken issue(UUID subject, Audience audience, String hashedToken,
                                     Instant now, Duration validity) {
        return new RefreshToken(RefreshTokenId.newId(), subject, audience, hashedToken,
            now, now.plus(validity), false);
    }

    public static RefreshToken rehydrate(RefreshTokenId id, UUID subject, Audience audience,
                                         String hashedToken, Instant issuedAt, Instant expiresAt, boolean revoked) {
        return new RefreshToken(id, subject, audience, hashedToken, issuedAt, expiresAt, revoked);
    }

    public void revoke() { this.revoked = true; }

    public boolean isActiveAt(Instant when) {
        return !revoked && when.isBefore(expiresAt);
    }

    public boolean isRevoked() { return revoked; }
    public UUID subject() { return subject; }
    public Audience audience() { return audience; }
    public String hashedToken() { return hashedToken; }
    public Instant issuedAt() { return issuedAt; }
    public Instant expiresAt() { return expiresAt; }
}
