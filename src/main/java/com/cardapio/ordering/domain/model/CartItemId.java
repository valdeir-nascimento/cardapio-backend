package com.cardapio.ordering.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CartItemId(UUID value) {
    public CartItemId { Objects.requireNonNull(value, "value"); }
    public static CartItemId newId() { return new CartItemId(UUID.randomUUID()); }
    public static CartItemId of(UUID value) { return new CartItemId(value); }
}
