package com.cardapio.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AddOnItemId(UUID value) {
    public AddOnItemId { Objects.requireNonNull(value, "value"); }
    public static AddOnItemId newId() { return new AddOnItemId(UUID.randomUUID()); }
    public static AddOnItemId of(UUID value) { return new AddOnItemId(value); }
    public static AddOnItemId of(String value) { return new AddOnItemId(UUID.fromString(value)); }
}
