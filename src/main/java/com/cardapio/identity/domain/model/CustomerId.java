package com.cardapio.identity.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CustomerId(UUID value) {
    public CustomerId { Objects.requireNonNull(value, "value"); }
    public static CustomerId newId() { return new CustomerId(UUID.randomUUID()); }
    public static CustomerId of(UUID value) { return new CustomerId(value); }
    public static CustomerId of(String value) { return new CustomerId(UUID.fromString(value)); }
}
