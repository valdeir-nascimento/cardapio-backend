package com.cardapio.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AddOnGroupId(UUID value) {
    public AddOnGroupId { Objects.requireNonNull(value, "value"); }
    public static AddOnGroupId newId() { return new AddOnGroupId(UUID.randomUUID()); }
    public static AddOnGroupId of(UUID value) { return new AddOnGroupId(value); }
    public static AddOnGroupId of(String value) { return new AddOnGroupId(UUID.fromString(value)); }
}
