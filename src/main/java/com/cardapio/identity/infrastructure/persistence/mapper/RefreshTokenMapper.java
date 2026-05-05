package com.cardapio.identity.infrastructure.persistence.mapper;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.RefreshTokenId;
import com.cardapio.identity.infrastructure.persistence.jpa.RefreshTokenJpaEntity;

public final class RefreshTokenMapper {
    private RefreshTokenMapper() {}

    public static RefreshTokenJpaEntity toJpa(RefreshToken t) {
        return new RefreshTokenJpaEntity(
            t.id().value(), t.subject(), t.audience().name(),
            t.hashedToken(), t.issuedAt(), t.expiresAt(), t.isRevoked());
    }

    public static void updateJpa(RefreshTokenJpaEntity entity, RefreshToken t) {
        entity.setRevoked(t.isRevoked());
    }

    public static RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return RefreshToken.rehydrate(
            RefreshTokenId.of(e.getId()), e.getSubject(),
            Audience.valueOf(e.getAudience()), e.getHashedToken(),
            e.getIssuedAt(), e.getExpiresAt(), e.isRevoked());
    }
}
