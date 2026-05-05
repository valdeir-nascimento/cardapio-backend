package com.cardapio.identity.domain.model;

import java.time.Instant;
import java.util.Objects;

public record TokenPair(
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken,
    Instant refreshTokenExpiresAt
) {
    public TokenPair {
        Objects.requireNonNull(accessToken, "accessToken");
        Objects.requireNonNull(accessTokenExpiresAt, "accessTokenExpiresAt");
        Objects.requireNonNull(refreshToken, "refreshToken");
        Objects.requireNonNull(refreshTokenExpiresAt, "refreshTokenExpiresAt");
    }
}
