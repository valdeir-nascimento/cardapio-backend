package com.cardapio.identity.infrastructure.persistence.mapper;

import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.SocialIdentity;
import com.cardapio.identity.domain.model.SocialProvider;
import com.cardapio.identity.infrastructure.persistence.jpa.CustomerJpaEntity;
import com.cardapio.identity.infrastructure.persistence.jpa.SocialIdentityJpaEntity;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class CustomerMapper {
    private CustomerMapper() {}

    public static CustomerJpaEntity toJpa(Customer c, Instant now) {
        CustomerJpaEntity entity = new CustomerJpaEntity(
            c.id().value(), c.name(), c.email().value(),
            c.phoneNumber().map(PhoneNumber::value).orElse(null),
            c.passwordHash().map(HashedPassword::value).orElse(null),
            now, now);
        entity.setSocialIdentities(toJpaSocialIdentities(c));
        return entity;
    }

    public static void updateJpa(CustomerJpaEntity entity, Customer c, Instant now) {
        entity.setName(c.name());
        entity.setPhoneNumber(c.phoneNumber().map(PhoneNumber::value).orElse(null));
        entity.setPasswordHash(c.passwordHash().map(HashedPassword::value).orElse(null));
        entity.setUpdatedAt(now);
        // Replace in place so Hibernate handles the diff via orphanRemoval.
        entity.getSocialIdentities().clear();
        entity.getSocialIdentities().addAll(toJpaSocialIdentities(c));
    }

    public static Customer toDomain(CustomerJpaEntity e) {
        PhoneNumber phone = e.getPhoneNumber() == null ? null : PhoneNumber.of(e.getPhoneNumber());
        HashedPassword hash = e.getPasswordHash() == null ? null : new HashedPassword(e.getPasswordHash());
        List<SocialIdentity> identities = e.getSocialIdentities().stream()
            .map(s -> new SocialIdentity(
                SocialProvider.valueOf(s.getProvider()),
                s.getSubject(),
                s.getEmailAtLink(),
                s.getLinkedAt()))
            .toList();
        return Customer.rehydrate(
            CustomerId.of(e.getId()),
            e.getName(),
            Email.of(e.getEmail()),
            phone,
            hash,
            identities);
    }

    private static List<SocialIdentityJpaEntity> toJpaSocialIdentities(Customer c) {
        List<SocialIdentityJpaEntity> out = new ArrayList<>();
        for (SocialIdentity s : c.socialIdentities()) {
            out.add(new SocialIdentityJpaEntity(
                c.id().value(),
                s.provider().name(),
                s.subject(),
                s.emailAtLink(),
                s.linkedAt()));
        }
        return out;
    }
}
