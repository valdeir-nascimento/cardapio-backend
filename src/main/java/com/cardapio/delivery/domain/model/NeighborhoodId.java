package com.cardapio.delivery.domain.model;

import java.util.Objects;
import java.util.UUID;

public record NeighborhoodId(UUID value) {
    public NeighborhoodId { Objects.requireNonNull(value, "value"); }
    public static NeighborhoodId newId() { return new NeighborhoodId(UUID.randomUUID()); }
    public static NeighborhoodId of(UUID value) { return new NeighborhoodId(value); }
    public static NeighborhoodId of(String value) { return new NeighborhoodId(UUID.fromString(value)); }
}
