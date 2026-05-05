package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.domain.model.*;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.identity.domain.port.TokenHasher;
import com.cardapio.identity.infrastructure.security.Sha256TokenHasher;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;
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

class LoginCustomerUseCaseTest {

    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final JwtIssuer issuer = mock(JwtIssuer.class);
    private final TokenHasher tokenHasher = new Sha256TokenHasher();
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-04T10:00:00Z"), ZoneId.of("UTC"));
    private final LoginCustomerUseCase useCase = new LoginCustomerUseCase(customers, hasher, issuer, refreshTokens, tokenHasher, clock);

    @Test
    void issuesTokenPairOnValidCredentials() {
        Customer c = Customer.rehydrate(
            CustomerId.newId(), "Maria", Email.of("maria@example.com"),
            PhoneNumber.of("+5511912345678"), new HashedPassword("$2a$12$x"));
        when(customers.findByEmail(Email.of("maria@example.com"))).thenReturn(Optional.of(c));
        when(hasher.matches(any(), any())).thenReturn(true);
        Instant now = clock.instant();
        when(issuer.issueAccessToken(any(), any(), any())).thenReturn(new JwtIssuer.IssuedJwt("access.jwt", now.plus(Duration.ofMinutes(15))));
        when(issuer.generateOpaqueRefreshToken()).thenReturn("refresh-token-raw");
        when(issuer.refreshTokenExpiry(any())).thenReturn(now.plus(Duration.ofDays(30)));

        Result<TokenPair> result = useCase.execute(new LoginCommand("maria@example.com", "S3curePass!"));

        assertThat(result.isSuccess()).isTrue();
        TokenPair pair = result.getOrThrow();
        assertThat(pair.accessToken()).isEqualTo("access.jwt");
        assertThat(pair.refreshToken()).isEqualTo("refresh-token-raw");
        verify(refreshTokens).save(any());
    }

    @Test
    void rejectsUnknownEmail() {
        when(customers.findByEmail(any())).thenReturn(Optional.empty());

        Result<TokenPair> result = useCase.execute(new LoginCommand("nope@example.com", "anything!1"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<TokenPair>) result).notification().errors())
            .extracting("code").contains("INVALID_CREDENTIALS");
    }

    @Test
    void rejectsWrongPassword() {
        Customer c = Customer.rehydrate(
            CustomerId.newId(), "X", Email.of("x@y.com"),
            PhoneNumber.of("+5511912345678"), new HashedPassword("$2a$12$x"));
        when(customers.findByEmail(any())).thenReturn(Optional.of(c));
        when(hasher.matches(any(), any())).thenReturn(false);

        Result<TokenPair> result = useCase.execute(new LoginCommand("x@y.com", "WrongPass1!"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<TokenPair>) result).notification().errors())
            .extracting("code").contains("INVALID_CREDENTIALS");
    }
}
