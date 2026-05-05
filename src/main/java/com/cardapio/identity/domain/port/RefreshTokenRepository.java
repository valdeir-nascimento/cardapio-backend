package com.cardapio.identity.domain.port;

import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.RefreshTokenId;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    void save(RefreshToken token);
    Optional<RefreshToken> findById(RefreshTokenId id);
    Optional<RefreshToken> findByHashedToken(String hashedToken);
    void revokeAllForSubject(UUID subject);
}
