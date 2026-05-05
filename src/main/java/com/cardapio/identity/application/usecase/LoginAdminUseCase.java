package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.domain.model.*;
import com.cardapio.identity.domain.port.AdminRepository;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class LoginAdminUseCase {

    private final AdminRepository admins;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher hasher;
    private final JwtIssuer issuer;
    private final Clock clock;

    public LoginAdminUseCase(AdminRepository admins, RefreshTokenRepository refreshTokens,
                             PasswordHasher hasher, JwtIssuer issuer, Clock clock) {
        this.admins = admins; this.refreshTokens = refreshTokens;
        this.hasher = hasher; this.issuer = issuer; this.clock = clock;
    }

    @Transactional
    public Result<TokenPair> execute(LoginCommand cmd) {
        Notification n = Notification.empty();
        Email email;
        try { email = Email.of(cmd.email()); }
        catch (RuntimeException e) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        Optional<Admin> maybe = admins.findByEmail(email);
        if (maybe.isEmpty()) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }
        Admin admin = maybe.get();

        RawPassword raw;
        try { raw = RawPassword.of(cmd.rawPassword()); }
        catch (RuntimeException e) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        if (!hasher.matches(raw, admin.passwordHash())) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        Instant now = clock.instant();
        var access = issuer.issueAccessToken(admin.id().value(), Audience.ADMIN, admin.roles());
        String rawRefresh = issuer.generateOpaqueRefreshToken();
        Instant refreshExp = issuer.refreshTokenExpiry(now);
        Duration ttl = Duration.between(now, refreshExp);

        RefreshToken token = RefreshToken.issue(admin.id().value(), Audience.ADMIN,
            LoginCustomerUseCase.sha256Hex(rawRefresh), now, ttl);
        refreshTokens.save(token);

        return Result.success(new TokenPair(access.token(), access.expiresAt(), rawRefresh, refreshExp));
    }
}
