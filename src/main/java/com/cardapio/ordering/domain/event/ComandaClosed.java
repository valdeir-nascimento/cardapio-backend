package com.cardapio.ordering.domain.event;

import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.TableId;
import com.cardapio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ComandaClosed(
    UUID id,
    Instant occurredOn,
    ComandaId comandaId,
    TableId tableId
) implements DomainEvent {

    public static ComandaClosed of(ComandaId comandaId, TableId tableId, Instant occurredOn) {
        return new ComandaClosed(UUID.randomUUID(), occurredOn, comandaId, tableId);
    }
}
