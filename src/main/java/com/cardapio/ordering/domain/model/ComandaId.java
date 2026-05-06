package com.cardapio.ordering.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ComandaId(UUID value) {
    public ComandaId { Objects.requireNonNull(value, "value"); }
    public static ComandaId newId() { return new ComandaId(UUID.randomUUID()); }
    public static ComandaId of(UUID value) { return new ComandaId(value); }
    public static ComandaId of(String value) { return new ComandaId(UUID.fromString(value)); }
}
