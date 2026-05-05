package com.cardapio.identity.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AdminId(UUID value) {
    public AdminId { Objects.requireNonNull(value, "value"); }
    public static AdminId newId() { return new AdminId(UUID.randomUUID()); }
    public static AdminId of(UUID value) { return new AdminId(value); }
    public static AdminId of(String value) { return new AdminId(UUID.fromString(value)); }
}
