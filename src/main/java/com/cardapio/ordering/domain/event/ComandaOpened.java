package com.cardapio.ordering.domain.event;

import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.TableId;
import com.cardapio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record ComandaOpened(
    UUID id,
    Instant occurredOn,
    ComandaId comandaId,
    TableId tableId,
    UUID openerCustomerId
) implements DomainEvent {

    public static ComandaOpened of(ComandaId comandaId, TableId tableId, UUID openerCustomerId, Instant occurredOn) {
        return new ComandaOpened(UUID.randomUUID(), occurredOn, comandaId, tableId, openerCustomerId);
    }
}
