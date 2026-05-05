package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.RefreshTokenCommand;
import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenUseCaseTest {

    private final RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
    private final JwtIssuer issuer = mock(JwtIssuer.class);
    private final Instant now = Instant.parse("2026-05-04T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneId.of("UTC"));
    private final RefreshTokenUseCase useCase = new RefreshTokenUseCase(repo, issuer, clock);

    @Test
    void rotatesValidToken() {
        UUID subject = UUID.randomUUID();
        RefreshToken stored = RefreshToken.issue(subject, Audience.CUSTOMER,
            LoginCustomerUseCase.sha256Hex("raw-refresh"), now.minus(Duration.ofMinutes(5)), Duration.ofDays(30));
        when(repo.findByHashedToken(LoginCustomerUseCase.sha256Hex("raw-refresh"))).thenReturn(Optional.of(stored));
        when(issuer.issueAccessToken(any(), any(), any())).thenReturn(new JwtIssuer.IssuedJwt("new-access", now.plus(Duration.ofMinutes(15))));
        when(issuer.generateOpaqueRefreshToken()).thenReturn("new-refresh");
        when(issuer.refreshTokenExpiry(any())).thenReturn(now.plus(Duration.ofDays(30)));

        Result<TokenPair> result = useCase.execute(new RefreshTokenCommand("raw-refresh"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(stored.isRevoked()).isTrue();  // old token revoked after rotation
        verify(repo, times(2)).save(any());  // save revoked old + save new
    }

    @Test
    void rejectsExpiredToken() {
        UUID subject = UUID.randomUUID();
        RefreshToken expired = RefreshToken.issue(subject, Audience.CUSTOMER,
            LoginCustomerUseCase.sha256Hex("raw"), now.minus(Duration.ofDays(40)), Duration.ofDays(30));
        when(repo.findByHashedToken(any())).thenReturn(Optional.of(expired));

        Result<TokenPair> result = useCase.execute(new RefreshTokenCommand("raw"));

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void rejectsUnknownToken() {
        when(repo.findByHashedToken(any())).thenReturn(Optional.empty());
        Result<TokenPair> result = useCase.execute(new RefreshTokenCommand("garbage"));
        assertThat(result.isSuccess()).isFalse();
    }
}
