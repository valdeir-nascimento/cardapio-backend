package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.domain.model.*;
import com.cardapio.identity.domain.port.AdminRepository;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.identity.domain.port.TokenHasher;
import com.cardapio.identity.infrastructure.security.Sha256TokenHasher;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LoginAdminUseCaseTest {

    private final AdminRepository admins = mock(AdminRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final JwtIssuer issuer = mock(JwtIssuer.class);
    private final TokenHasher tokenHasher = new Sha256TokenHasher();
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-04T10:00:00Z"), ZoneId.of("UTC"));
    private final LoginAdminUseCase useCase = new LoginAdminUseCase(admins, hasher, issuer, refreshTokens, tokenHasher, clock);

    @Test
    void issuesTokenWithAdminAudienceAndRoles() {
        Admin admin = Admin.rehydrate(AdminId.newId(), "Boss", Email.of("boss@cardapio.com"),
            new HashedPassword("$2a$12$x"), Set.of(Role.OWNER));
        when(admins.findByEmail(any())).thenReturn(Optional.of(admin));
        when(hasher.matches(any(), any())).thenReturn(true);
        when(issuer.issueAccessToken(any(), eq(Audience.ADMIN), eq(Set.of(Role.OWNER))))
            .thenReturn(new JwtIssuer.IssuedJwt("admin.jwt", clock.instant().plus(Duration.ofMinutes(15))));
        when(issuer.generateOpaqueRefreshToken()).thenReturn("rfsh");
        when(issuer.refreshTokenExpiry(any())).thenReturn(clock.instant().plus(Duration.ofDays(30)));

        Result<TokenPair> result = useCase.execute(new LoginCommand("boss@cardapio.com", "S3curePass!"));

        assertThat(result.isSuccess()).isTrue();
        verify(issuer).issueAccessToken(any(), eq(Audience.ADMIN), eq(Set.of(Role.OWNER)));
    }

    @Test
    void rejectsUnknownAdmin() {
        when(admins.findByEmail(any())).thenReturn(Optional.empty());
        Result<TokenPair> result = useCase.execute(new LoginCommand("nope@x.com", "Anything!1"));
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void rejectsWrongPassword() {
        Admin admin = Admin.rehydrate(AdminId.newId(), "X", Email.of("x@y.com"),
            new HashedPassword("$2a$12$x"), Set.of(Role.MANAGER));
        when(admins.findByEmail(any())).thenReturn(Optional.of(admin));
        when(hasher.matches(any(), any())).thenReturn(false);
        Result<TokenPair> result = useCase.execute(new LoginCommand("x@y.com", "WrongPass1!"));
        assertThat(result.isSuccess()).isFalse();
    }
}
