package com.cardapio.ordering.domain.model;

import java.util.Objects;
import java.util.UUID;

public record TableId(UUID value) {
    public TableId { Objects.requireNonNull(value, "value"); }
    public static TableId newId() { return new TableId(UUID.randomUUID()); }
    public static TableId of(UUID value) { return new TableId(value); }
    public static TableId of(String value) { return new TableId(UUID.fromString(value)); }
}
