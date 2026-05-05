package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.domain.model.*;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.identity.domain.port.TokenHasher;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractLoginUseCase<U> {

    private final PasswordHasher hasher;
    private final JwtIssuer issuer;
    private final RefreshTokenRepository refreshTokens;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    protected AbstractLoginUseCase(
        PasswordHasher hasher,
        JwtIssuer issuer,
        RefreshTokenRepository refreshTokens,
        TokenHasher tokenHasher,
        Clock clock
    ) {
        this.hasher = hasher;
        this.issuer = issuer;
        this.refreshTokens = refreshTokens;
        this.tokenHasher = tokenHasher;
        this.clock = clock;
    }

    protected abstract Optional<U> findUser(Email email);

    protected abstract HashedPassword passwordOf(U user);

    protected abstract UUID subjectOf(U user);

    protected abstract Audience audienceOf(U user);

    protected abstract Set<Role> rolesOf(U user);

    public final Result<TokenPair> execute(LoginCommand cmd) {
        Email email;
        try {
            email = Email.of(cmd.email());
        } catch (RuntimeException e) {
            return Result.failWith(ErrorCode.INVALID_CREDENTIALS);
        }

        Optional<U> maybe = findUser(email);
        if (maybe.isEmpty()) {
            return Result.failWith(ErrorCode.INVALID_CREDENTIALS);
        }

        U user = maybe.get();
        RawPassword raw;
        try {
            raw = RawPassword.of(cmd.rawPassword());
        } catch (RuntimeException e) {
            return Result.failWith(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!hasher.matches(raw, passwordOf(user))) {
            return Result.failWith(ErrorCode.INVALID_CREDENTIALS);
        }

        Instant now = clock.instant();
        var access = issuer.issueAccessToken(subjectOf(user), audienceOf(user), rolesOf(user));
        String rawRefresh = issuer.generateOpaqueRefreshToken();
        Instant refreshExp = issuer.refreshTokenExpiry(now);

        RefreshToken token = RefreshToken.issue(
            subjectOf(user), audienceOf(user),
            tokenHasher.sha256Hex(rawRefresh), now, Duration.between(now, refreshExp));
        refreshTokens.save(token);

        return Result.success(new TokenPair(access.token(), access.expiresAt(), rawRefresh, refreshExp));
    }
}
