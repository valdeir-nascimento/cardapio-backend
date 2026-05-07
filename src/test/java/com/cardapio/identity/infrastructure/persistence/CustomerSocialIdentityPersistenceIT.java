package com.cardapio.identity.infrastructure.persistence;

import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.SocialIdentity;
import com.cardapio.identity.domain.model.SocialProvider;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class CustomerSocialIdentityPersistenceIT {

    @Autowired CustomerRepository customers;

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void emailPasswordCustomerRoundTrip() {
        Customer c = Customer.register("Maria " + suffix(),
            Email.of("maria-" + suffix() + "@example.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$dummy"));
        customers.save(c);

        var loaded = customers.findById(c.id()).orElseThrow();
        assertThat(loaded.passwordHash()).isPresent();
        assertThat(loaded.phoneNumber()).isPresent();
        assertThat(loaded.socialIdentities()).isEmpty();
    }

    @Test
    void socialOnlyCustomerRoundTrip() {
        String suffix = suffix();
        SocialIdentity gid = new SocialIdentity(SocialProvider.GOOGLE, "g-" + suffix,
            "bob-" + suffix + "@example.com", clock.instant());
        Customer c = Customer.registerSocial("Bob " + suffix,
            Email.of("bob-" + suffix + "@example.com"), gid);
        customers.save(c);

        var loaded = customers.findById(c.id()).orElseThrow();
        assertThat(loaded.passwordHash()).isEmpty();
        assertThat(loaded.phoneNumber()).isEmpty();
        assertThat(loaded.socialIdentities()).hasSize(1);
        assertThat(loaded.findSocialIdentity(SocialProvider.GOOGLE)).contains(gid);
    }

    @Test
    void findBySocialIdentityResolvesCustomer() {
        String suffix = suffix();
        String subject = "g-" + suffix;
        SocialIdentity gid = new SocialIdentity(SocialProvider.GOOGLE, subject,
            "alice-" + suffix + "@example.com", clock.instant());
        Customer c = Customer.registerSocial("Alice " + suffix,
            Email.of("alice-" + suffix + "@example.com"), gid);
        customers.save(c);

        var found = customers.findBySocialIdentity(SocialProvider.GOOGLE, subject);
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(c.id());
    }

    @Test
    void uniqueProviderSubjectBlocksTwoCustomersFromClaimingSameSocialId() {
        String subject = "shared-" + suffix();
        SocialIdentity gid = new SocialIdentity(SocialProvider.GOOGLE, subject, "a@x.com", clock.instant());
        Customer first = Customer.registerSocial("First", Email.of("first-" + suffix() + "@x.com"), gid);
        customers.save(first);

        Customer second = Customer.registerSocial("Second", Email.of("second-" + suffix() + "@x.com"),
            new SocialIdentity(SocialProvider.GOOGLE, subject, "b@x.com", clock.instant()));
        assertThatThrownBy(() -> customers.save(second))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private String suffix() {
        return Integer.toHexString(Math.abs((int) System.nanoTime())).toUpperCase();
    }
}
