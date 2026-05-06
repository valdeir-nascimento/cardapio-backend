package com.cardapio.ordering.domain.event;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderCanceled(
    UUID id,
    Instant occurredOn,
    OrderId orderId,
    UUID customerId,
    String couponCode
) implements DomainEvent {

    public static OrderCanceled of(OrderId orderId, UUID customerId, Instant occurredOn) {
        return of(orderId, customerId, null, occurredOn);
    }

    public static OrderCanceled of(OrderId orderId, UUID customerId, String couponCode, Instant occurredOn) {
        return new OrderCanceled(UUID.randomUUID(), occurredOn, orderId, customerId, couponCode);
    }
}
