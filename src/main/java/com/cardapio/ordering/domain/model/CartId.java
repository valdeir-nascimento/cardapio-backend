package com.cardapio.ordering.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CartId(UUID value) {
    public CartId { Objects.requireNonNull(value, "value"); }
    public static CartId newId() { return new CartId(UUID.randomUUID()); }
    public static CartId of(UUID value) { return new CartId(value); }
    public static CartId of(String value) { return new CartId(UUID.fromString(value)); }
}
