package com.cardapio.ordering.domain.event;

import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ComandaJoined(
    UUID id,
    Instant occurredOn,
    ComandaId comandaId,
    UUID customerId
) implements DomainEvent {

    public static ComandaJoined of(ComandaId comandaId, UUID customerId, Instant occurredOn) {
        return new ComandaJoined(UUID.randomUUID(), occurredOn, comandaId, customerId);
    }
}
