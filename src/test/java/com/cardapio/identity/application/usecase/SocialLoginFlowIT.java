package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.LoginWithSocialIdentityCommand;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.SocialProvider;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.IdTokenVerifier;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import com.cardapio.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgresTestContainerConfig.class, SocialLoginFlowIT.StubbedVerifierConfig.class})
class SocialLoginFlowIT {

    @Autowired LoginWithSocialIdentityUseCase useCase;
    @Autowired CustomerRepository customers;
    @Autowired StubbedGoogleVerifier googleVerifier;

    private String suffix;

    @BeforeEach
    void resetVerifier() {
        suffix = Integer.toHexString(Math.abs((int) System.nanoTime())).toUpperCase();
    }

    @Test
    void firstLoginCreatesSocialOnlyCustomer() {
        String email = "alice-" + suffix + "@example.com";
        googleVerifier.next("g-" + suffix, email, "Alice");

        Result<TokenPair> result = useCase.execute(
            new LoginWithSocialIdentityCommand(SocialProvider.GOOGLE, "stub.token"));

        assertThat(result.isSuccess()).isTrue();
        Customer loaded = customers.findByEmail(Email.of(email)).orElseThrow();
        assertThat(loaded.passwordHash()).isEmpty();
        assertThat(loaded.findSocialIdentity(SocialProvider.GOOGLE)).isPresent();
    }

    @Test
    void secondLoginReusesSameCustomerById() {
        String email = "bob-" + suffix + "@example.com";
        googleVerifier.next("g-" + suffix, email, "Bob");
        useCase.execute(new LoginWithSocialIdentityCommand(SocialProvider.GOOGLE, "stub.token"));
        UUID first = customers.findByEmail(Email.of(email)).orElseThrow().id().value();

        googleVerifier.next("g-" + suffix, email, "Bob");
        useCase.execute(new LoginWithSocialIdentityCommand(SocialProvider.GOOGLE, "stub.token"));

        UUID second = customers.findByEmail(Email.of(email)).orElseThrow().id().value();
        assertThat(second).isEqualTo(first);
    }

    @Test
    void linksIntoExistingEmailPasswordCustomer() {
        String email = "carol-" + suffix + "@example.com";
        Customer existing = Customer.register("Carol",
            Email.of(email),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$dummy"));
        customers.save(existing);

        googleVerifier.next("g-link-" + suffix, email, "Carol");

        Result<TokenPair> result = useCase.execute(
            new LoginWithSocialIdentityCommand(SocialProvider.GOOGLE, "stub.token"));

        assertThat(result.isSuccess()).isTrue();
        Customer linked = customers.findByEmail(Email.of(email)).orElseThrow();
        assertThat(linked.id().value()).isEqualTo(existing.id().value());
        assertThat(linked.findSocialIdentity(SocialProvider.GOOGLE)).isPresent();
        assertThat(linked.passwordHash()).isPresent();
    }

    static class StubbedGoogleVerifier implements IdTokenVerifier {
        private VerifiedIdToken next;

        void next(String subject, String email, String name) {
            this.next = new VerifiedIdToken(subject, email, name);
        }

        @Override public SocialProvider provider() { return SocialProvider.GOOGLE; }

        @Override public VerifiedIdToken verify(String idToken) {
            if (next == null) throw new InvalidIdTokenException(
                InvalidIdTokenException.Reason.MALFORMED, "no token primed");
            VerifiedIdToken out = next;
            next = null;
            return out;
        }
    }

    @TestConfiguration
    static class StubbedVerifierConfig {
        @Bean StubbedGoogleVerifier googleVerifier() {
            return new StubbedGoogleVerifier();
        }
    }
}
