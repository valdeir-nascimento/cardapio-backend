package com.cardapio.identity.api.dto;

import com.cardapio.identity.domain.model.TokenPair;

import java.time.Instant;

public record TokenPairResponse(
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken,
    Instant refreshTokenExpiresAt
) {
    public static TokenPairResponse from(TokenPair t) {
        return new TokenPairResponse(t.accessToken(), t.accessTokenExpiresAt(),
            t.refreshToken(), t.refreshTokenExpiresAt());
    }
}
