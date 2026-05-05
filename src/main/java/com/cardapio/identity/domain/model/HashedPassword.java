package com.cardapio.identity.domain.model;

import java.util.Objects;

public record HashedPassword(String value) {
    public HashedPassword {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("hashed password must not be blank");
        }
    }
    public static HashedPassword of(String value) { return new HashedPassword(value); }
    @Override public String toString() { return "HashedPassword[***]"; }
}
