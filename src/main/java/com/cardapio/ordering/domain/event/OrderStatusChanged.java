package com.cardapio.ordering.domain.event;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusChanged(
    UUID id,
    Instant occurredOn,
    OrderId orderId,
    OrderStatus from,
    OrderStatus to
) implements DomainEvent {

    public static OrderStatusChanged of(OrderId orderId, OrderStatus from, OrderStatus to, Instant occurredOn) {
        return new OrderStatusChanged(UUID.randomUUID(), occurredOn, orderId, from, to);
    }
}
