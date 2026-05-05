package com.cardapio.identity.infrastructure.persistence.mapper;

import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.infrastructure.persistence.jpa.CustomerJpaEntity;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;

import java.time.Instant;

public final class CustomerMapper {
    private CustomerMapper() {}

    public static CustomerJpaEntity toJpa(Customer c, Instant now) {
        return new CustomerJpaEntity(
            c.id().value(), c.name(), c.email().value(), c.phoneNumber().value(),
            c.passwordHash().value(), now, now);
    }

    public static void updateJpa(CustomerJpaEntity entity, Customer c, Instant now) {
        entity.setName(c.name());
        entity.setPhoneNumber(c.phoneNumber().value());
        entity.setPasswordHash(c.passwordHash().value());
        entity.setUpdatedAt(now);
    }

    public static Customer toDomain(CustomerJpaEntity e) {
        return Customer.rehydrate(
            CustomerId.of(e.getId()),
            e.getName(),
            Email.of(e.getEmail()),
            PhoneNumber.of(e.getPhoneNumber()),
            new HashedPassword(e.getPasswordHash()));
    }
}
