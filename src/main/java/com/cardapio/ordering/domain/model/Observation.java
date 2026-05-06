package com.cardapio.ordering.domain.model;

import java.util.Objects;

public record Observation(String value) {
    public static final int MAX_LENGTH = 200;

    public Observation {
        Objects.requireNonNull(value, "value");
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("observation must be at most " + MAX_LENGTH + " chars");
        }
    }

    public static Observation empty() { return new Observation(""); }

    public static Observation of(String raw) {
        if (raw == null) return empty();
        return new Observation(raw.trim());
    }

    public boolean isEmpty() { return value.isEmpty(); }
}
