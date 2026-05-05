package com.cardapio.identity.domain.event;

import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.shared.domain.DomainEvent;
import com.cardapio.shared.domain.Email;

import java.time.Instant;
import java.util.UUID;

public record CustomerRegistered(
    UUID id,
    Instant occurredOn,
    CustomerId customerId,
    Email email
) implements DomainEvent {

    public static CustomerRegistered now(CustomerId customerId, Email email) {
        return new CustomerRegistered(UUID.randomUUID(), Instant.now(), customerId, email);
    }
}
