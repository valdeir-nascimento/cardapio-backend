package com.cardapio.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CategoryId(UUID value) {
    public CategoryId { Objects.requireNonNull(value, "value"); }
    public static CategoryId newId() { return new CategoryId(UUID.randomUUID()); }
    public static CategoryId of(UUID value) { return new CategoryId(value); }
    public static CategoryId of(String value) { return new CategoryId(UUID.fromString(value)); }
}
