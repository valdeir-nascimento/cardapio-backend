package com.cardapio.identity.infrastructure.persistence.adapter;

import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.RefreshTokenId;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.identity.infrastructure.persistence.mapper.RefreshTokenMapper;
import com.cardapio.identity.infrastructure.persistence.repository.SpringRefreshTokenJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final SpringRefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(SpringRefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(RefreshToken token) {
        var existing = jpa.findById(token.id().value());
        if (existing.isPresent()) {
            RefreshTokenMapper.updateJpa(existing.get(), token);
            jpa.save(existing.get());
        } else {
            jpa.save(RefreshTokenMapper.toJpa(token));
        }
    }

    @Override
    public Optional<RefreshToken> findById(RefreshTokenId id) {
        return jpa.findById(id.value()).map(RefreshTokenMapper::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByHashedToken(String hashedToken) {
        return jpa.findByHashedToken(hashedToken).map(RefreshTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllForSubject(UUID subject) {
        jpa.revokeAllForSubject(subject);
    }
}
