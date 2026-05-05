package com.cardapio.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

public record VariationId(UUID value) {
    public VariationId { Objects.requireNonNull(value, "value"); }
    public static VariationId newId() { return new VariationId(UUID.randomUUID()); }
    public static VariationId of(UUID value) { return new VariationId(value); }
    public static VariationId of(String value) { return new VariationId(UUID.fromString(value)); }
}
