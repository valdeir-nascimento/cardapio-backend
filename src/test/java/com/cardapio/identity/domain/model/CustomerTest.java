package com.cardapio.identity.domain.model;

import com.cardapio.identity.domain.event.CustomerRegistered;
import com.cardapio.shared.domain.DomainEvent;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTest {

    @Test
    void registersANewCustomerEmittingEvent() {
        Customer customer = Customer.register(
            "Maria Silva",
            Email.of("maria@example.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$dummyhash"));

        assertThat(customer.id()).isNotNull();
        assertThat(customer.name()).isEqualTo("Maria Silva");
        assertThat(customer.email().value()).isEqualTo("maria@example.com");

        List<DomainEvent> events = customer.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(CustomerRegistered.class);
    }

    @Test
    void changesNameAndPhone() {
        Customer customer = Customer.register(
            "Old Name",
            Email.of("a@b.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$x"));
        customer.pullDomainEvents();  // drain registration event

        customer.updateProfile("New Name", PhoneNumber.of("+5511987654321"));

        assertThat(customer.name()).isEqualTo("New Name");
        assertThat(customer.phoneNumber().orElseThrow().value()).isEqualTo("+5511987654321");
    }

    @Test
    void registerSocialProducesCustomerWithoutPasswordOrPhone() {
        SocialIdentity gid = new SocialIdentity(SocialProvider.GOOGLE, "g-1",
            "bob@example.com", java.time.Instant.parse("2026-05-06T12:00:00Z"));

        Customer customer = Customer.registerSocial("Bob", Email.of("bob@example.com"), gid);

        assertThat(customer.passwordHash()).isEmpty();
        assertThat(customer.phoneNumber()).isEmpty();
        assertThat(customer.socialIdentities()).containsExactly(gid);
        assertThat(customer.findSocialIdentity(SocialProvider.GOOGLE)).contains(gid);
    }

    @Test
    void linkSocialIdentityIsIdempotentForExactMatchAndRejectsDifferentSubject() {
        Customer customer = Customer.register(
            "C", Email.of("c@x.com"), PhoneNumber.of("+5511912345678"), HashedPassword.of("$2a$12$x"));
        java.time.Clock clock = java.time.Clock.fixed(java.time.Instant.parse("2026-05-06T12:00:00Z"), java.time.ZoneOffset.UTC);
        SocialIdentity gid1 = new SocialIdentity(SocialProvider.GOOGLE, "g-1", "c@x.com", clock.instant());

        customer.linkSocialIdentity(gid1, clock);
        customer.linkSocialIdentity(gid1, clock); // exact dup → no-op

        assertThat(customer.socialIdentities()).hasSize(1);

        SocialIdentity gid2 = new SocialIdentity(SocialProvider.GOOGLE, "g-2", "c@x.com", clock.instant());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> customer.linkSocialIdentity(gid2, clock))
            .isInstanceOf(com.cardapio.identity.domain.exception.SocialIdentityAlreadyLinkedException.class);
    }

    @Test
    void rehydrateRejectsCustomerWithNeitherPasswordNorSocial() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> Customer.rehydrate(
            CustomerId.newId(), "X", Email.of("x@y.com"), null, null))
            .isInstanceOf(com.cardapio.identity.domain.exception.CustomerWithoutAuthMethodException.class);
    }

    @Test
    void anonymizeWipesPiiAndMarksDeleted() {
        java.time.Clock clock = java.time.Clock.fixed(java.time.Instant.parse("2026-05-06T12:00:00Z"), java.time.ZoneOffset.UTC);
        Customer customer = Customer.register("Maria Silva",
            Email.of("maria@example.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$hash"));

        customer.anonymize(clock);

        assertThat(customer.isDeleted()).isTrue();
        assertThat(customer.deletedAt()).contains(clock.instant());
        assertThat(customer.name()).startsWith("deleted-user-");
        assertThat(customer.email().value()).startsWith("deleted+").endsWith("@cardapio.local");
        assertThat(customer.phoneNumber()).isEmpty();
        assertThat(customer.passwordHash()).isEmpty();
        assertThat(customer.socialIdentities()).isEmpty();
    }

    @Test
    void anonymizeIsIdempotent() {
        java.time.Clock first = java.time.Clock.fixed(java.time.Instant.parse("2026-05-06T12:00:00Z"), java.time.ZoneOffset.UTC);
        java.time.Clock later = java.time.Clock.fixed(java.time.Instant.parse("2026-06-01T12:00:00Z"), java.time.ZoneOffset.UTC);
        Customer customer = Customer.register("X", Email.of("x@y.com"),
            PhoneNumber.of("+5511912345678"), HashedPassword.of("$2a$12$hash"));

        customer.anonymize(first);
        String anonymizedEmail = customer.email().value();
        customer.anonymize(later); // second call should be a no-op

        assertThat(customer.deletedAt()).contains(first.instant());
        assertThat(customer.email().value()).isEqualTo(anonymizedEmail);
    }

    @Test
    void rehydrateAcceptsAnonymizedCustomerWithoutAuth() {
        Customer customer = Customer.rehydrate(
            CustomerId.newId(),
            "deleted-user-12345678",
            Email.of("deleted+12345678@cardapio.local"),
            null, null,
            java.util.List.of(),
            java.time.Instant.parse("2026-05-06T12:00:00Z"));

        assertThat(customer.isDeleted()).isTrue();
        assertThat(customer.passwordHash()).isEmpty();
        assertThat(customer.socialIdentities()).isEmpty();
    }

    @Test
    void rehydratesFromPersistence() {
        CustomerId id = CustomerId.newId();
        Customer customer = Customer.rehydrate(
            id,
            "Maria",
            Email.of("maria@example.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$x"));

        assertThat(customer.id()).isEqualTo(id);
        assertThat(customer.pullDomainEvents()).isEmpty();  // no event on rehydrate
    }
}
