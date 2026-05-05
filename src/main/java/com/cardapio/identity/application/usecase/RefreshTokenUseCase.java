package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.RefreshTokenCommand;
import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.identity.domain.port.TokenHasher;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final RefreshTokenRepository repo;
    private final JwtIssuer issuer;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    @Transactional
    public Result<TokenPair> execute(RefreshTokenCommand cmd) {
        Instant now = clock.instant();
        String hashed = tokenHasher.sha256Hex(cmd.refreshToken());

        Optional<RefreshToken> maybe = repo.findByHashedToken(hashed);
        if (maybe.isEmpty()) {
            return Result.failWith(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken old = maybe.get();
        if (!old.isActiveAt(now)) {
            return Result.failWith(ErrorCode.INVALID_REFRESH_TOKEN, "refresh token expirado ou revogado");
        }

        old.revoke();
        repo.save(old);

        final var access = issuer.issueAccessToken(old.subject(), old.audience(), Set.of());

        String rawNew = issuer.generateOpaqueRefreshToken();
        Instant newExp = issuer.refreshTokenExpiry(now);
        Duration ttl = Duration.between(now, newExp);
        RefreshToken next = RefreshToken.issue(old.subject(), old.audience(), tokenHasher.sha256Hex(rawNew), now, ttl);

        repo.save(next);

        return Result.success(new TokenPair(access.token(), access.expiresAt(), rawNew, newExp));
    }
}
