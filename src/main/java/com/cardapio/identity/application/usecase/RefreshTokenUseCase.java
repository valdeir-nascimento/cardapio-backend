package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.RefreshTokenCommand;
import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Service
public class RefreshTokenUseCase {

    private final RefreshTokenRepository repo;
    private final JwtIssuer issuer;
    private final Clock clock;

    public RefreshTokenUseCase(RefreshTokenRepository repo, JwtIssuer issuer, Clock clock) {
        this.repo = repo; this.issuer = issuer; this.clock = clock;
    }

    @Transactional
    public Result<TokenPair> execute(RefreshTokenCommand cmd) {
        Notification n = Notification.empty();
        Instant now = clock.instant();
        String hashed = LoginCustomerUseCase.sha256Hex(cmd.refreshToken());

        Optional<RefreshToken> maybe = repo.findByHashedToken(hashed);
        if (maybe.isEmpty()) {
            n.addError("INVALID_REFRESH_TOKEN", "refresh token inválido");
            return Result.failure(n);
        }
        RefreshToken old = maybe.get();
        if (!old.isActiveAt(now)) {
            n.addError("INVALID_REFRESH_TOKEN", "refresh token expirado ou revogado");
            return Result.failure(n);
        }

        // rotate: revoke old, issue new
        old.revoke();
        repo.save(old);

        var access = issuer.issueAccessToken(old.subject(), old.audience(), Set.of());
        String rawNew = issuer.generateOpaqueRefreshToken();
        Instant newExp = issuer.refreshTokenExpiry(now);
        Duration ttl = Duration.between(now, newExp);
        RefreshToken next = RefreshToken.issue(old.subject(), old.audience(),
            LoginCustomerUseCase.sha256Hex(rawNew), now, ttl);
        repo.save(next);

        return Result.success(new TokenPair(access.token(), access.expiresAt(), rawNew, newExp));
    }
}
