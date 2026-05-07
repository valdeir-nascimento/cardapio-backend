package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.LoginWithSocialIdentityCommand;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.SocialProvider;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.IdTokenVerifier;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.identity.infrastructure.security.Sha256TokenHasher;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginWithSocialIdentityUseCaseTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T12:00:00Z"), ZoneOffset.UTC);
    private final InMemoryCustomerRepo customers = new InMemoryCustomerRepo();
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final JwtIssuer issuer = mock(JwtIssuer.class);
    private final StubVerifier googleVerifier = new StubVerifier(SocialProvider.GOOGLE);

    private LoginWithSocialIdentityUseCase newUseCase(IdTokenVerifier... verifiers) {
        Instant now = clock.instant();
        when(issuer.issueAccessToken(any(), any(), any()))
            .thenReturn(new JwtIssuer.IssuedJwt("access.jwt", now.plus(Duration.ofMinutes(15))));
        when(issuer.generateOpaqueRefreshToken()).thenReturn("refresh-raw");
        when(issuer.refreshTokenExpiry(any())).thenReturn(now.plus(Duration.ofDays(30)));
        return new LoginWithSocialIdentityUseCase(
            List.of(verifiers), customers, issuer, refreshTokens, new Sha256TokenHasher(), clock);
    }

    @Test
    void firstLoginCreatesCustomer() {
        var useCase = newUseCase(googleVerifier);
        googleVerifier.next("google-1", "alice@example.com", "Alice");

        Result<TokenPair> result = useCase.execute(
            new LoginWithSocialIdentityCommand(SocialProvider.GOOGLE, "id-token"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(customers.byEmail.get("alice@example.com")).isNotNull();
        verify(refreshTokens).save(any());
    }

    @Test
    void secondLoginReusesSameCustomer() {
        var useCase = newUseCase(googleVerifier);
        googleVerifier.next("google-1", "alice@example.com", "Alice");
        useCase.execute(new LoginWithSocialIdentityCommand(SocialProvider.GOOGLE, "id-token"));
        UUID firstId = customers.byEmail.get("alice@example.com").id().value();

        googleVerifier.next("google-1", "alice@example.com", "Alice");
        useCase.execute(new LoginWithSocialIdentityCommand(SocialProvider.GOOGLE, "id-token"));

        assertThat(customers.byEmail).hasSize(1);
        assertThat(customers.byEmail.get("alice@example.com").id().value()).isEqualTo(firstId);
    }

    @Test
    void linksSocialIdentityIntoExistingEmailPasswordCustomer() {
        // Pre-existing email/password customer
        Customer existing = Customer.register("Bob",
            Email.of("bob@example.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$dummy"));
        customers.save(existing);

        var useCase = newUseCase(googleVerifier);
        googleVerifier.next("google-bob", "bob@example.com", "Bob");

        Result<TokenPair> result = useCase.execute(
            new LoginWithSocialIdentityCommand(SocialProvider.GOOGLE, "id-token"));

        assertThat(result.isSuccess()).isTrue();
        Customer linked = customers.byEmail.get("bob@example.com");
        assertThat(linked.id()).isEqualTo(existing.id());
        assertThat(linked.findSocialIdentity(SocialProvider.GOOGLE)).isPresent();
        assertThat(linked.passwordHash()).isPresent(); // password preserved
    }

    @Test
    void rejectsWhenProviderIsDisabled() {
        var useCase = newUseCase(); // no verifiers registered

        Result<TokenPair> result = useCase.execute(
            new LoginWithSocialIdentityCommand(SocialProvider.APPLE, "id-token"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<TokenPair>) result).notification().errors())
            .extracting("code").contains("SOCIAL_LOGIN_PROVIDER_UNAVAILABLE");
    }

    @Test
    void rejectsInvalidToken() {
        var useCase = newUseCase(googleVerifier);
        googleVerifier.fail(new IdTokenVerifier.InvalidIdTokenException(
            IdTokenVerifier.InvalidIdTokenException.Reason.EXPIRED, "expired"));

        Result<TokenPair> result = useCase.execute(
            new LoginWithSocialIdentityCommand(SocialProvider.GOOGLE, "id-token"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<TokenPair>) result).notification().errors())
            .extracting("code").contains("INVALID_ID_TOKEN");
    }

    private static class StubVerifier implements IdTokenVerifier {
        private final SocialProvider provider;
        private VerifiedIdToken next;
        private InvalidIdTokenException error;

        StubVerifier(SocialProvider provider) { this.provider = provider; }

        void next(String subject, String email, String name) {
            this.next = new VerifiedIdToken(subject, email, name);
            this.error = null;
        }

        void fail(InvalidIdTokenException error) {
            this.error = error;
            this.next = null;
        }

        @Override public SocialProvider provider() { return provider; }

        @Override public VerifiedIdToken verify(String idToken) {
            if (error != null) throw error;
            return next;
        }
    }

    private static class InMemoryCustomerRepo implements CustomerRepository {
        final Map<UUID, Customer> byId = new HashMap<>();
        final Map<String, Customer> byEmail = new HashMap<>();

        @Override public void save(Customer customer) {
            byId.put(customer.id().value(), customer);
            byEmail.put(customer.email().value(), customer);
        }
        @Override public Optional<Customer> findById(com.cardapio.identity.domain.model.CustomerId id) {
            return Optional.ofNullable(byId.get(id.value()));
        }
        @Override public Optional<Customer> findByEmail(Email email) {
            return Optional.ofNullable(byEmail.get(email.value()));
        }
        @Override public boolean existsByEmail(Email email) {
            return byEmail.containsKey(email.value());
        }
        @Override public Optional<Customer> findBySocialIdentity(SocialProvider provider, String subject) {
            return byId.values().stream()
                .filter(c -> c.socialIdentities().stream()
                    .anyMatch(s -> s.provider() == provider && s.subject().equals(subject)))
                .findFirst();
        }
    }
}
