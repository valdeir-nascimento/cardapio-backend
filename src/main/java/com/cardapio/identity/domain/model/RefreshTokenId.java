package com.cardapio.identity.domain.model;

import java.util.Objects;
import java.util.UUID;

public record RefreshTokenId(UUID value) {
    public RefreshTokenId { Objects.requireNonNull(value, "value"); }
    public static RefreshTokenId newId() { return new RefreshTokenId(UUID.randomUUID()); }
    public static RefreshTokenId of(UUID value) { return new RefreshTokenId(value); }
}
