package com.cardapio.ordering.domain.event;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.shared.domain.DomainEvent;
import com.cardapio.shared.domain.Money;

import java.time.Instant;
import java.util.UUID;

public record OrderPlaced(
    UUID id,
    Instant occurredOn,
    OrderId orderId,
    UUID customerId,
    OrderModality modality,
    Money total,
    String couponCode
) implements DomainEvent {

    public static OrderPlaced of(OrderId orderId, UUID customerId, OrderModality modality, Money total, Instant occurredOn) {
        return of(orderId, customerId, modality, total, null, occurredOn);
    }

    public static OrderPlaced of(OrderId orderId, UUID customerId, OrderModality modality, Money total,
                                 String couponCode, Instant occurredOn) {
        return new OrderPlaced(UUID.randomUUID(), occurredOn, orderId, customerId, modality, total, couponCode);
    }
}
