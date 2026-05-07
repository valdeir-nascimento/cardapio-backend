package com.cardapio.review.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ReviewId(UUID value) {
    public ReviewId { Objects.requireNonNull(value, "value"); }
    public static ReviewId newId() { return new ReviewId(UUID.randomUUID()); }
    public static ReviewId of(UUID value) { return new ReviewId(value); }
    public static ReviewId of(String value) { return new ReviewId(UUID.fromString(value)); }
}
