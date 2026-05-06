package com.cardapio.ordering.domain.model;

import java.util.Objects;
import java.util.UUID;

public record OrderItemId(UUID value) {
    public OrderItemId { Objects.requireNonNull(value, "value"); }
    public static OrderItemId newId() { return new OrderItemId(UUID.randomUUID()); }
    public static OrderItemId of(UUID value) { return new OrderItemId(value); }
}
