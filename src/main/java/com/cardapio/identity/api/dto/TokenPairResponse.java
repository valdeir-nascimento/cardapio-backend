package com.cardapio.identity.api.dto;

import com.cardapio.identity.domain.model.TokenPair;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Par de tokens JWT emitido após login bem-sucedido.")
public record TokenPairResponse(

    @Schema(
        description = "Token de acesso JWT. Envie em todas as chamadas autenticadas no header `Authorization: Bearer <accessToken>`. Curta duração (~15 min).",
        example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjMmI0MTIxNi0xNGY4LTRkMmMtYTBkOC0zNGI5N2RjN2I4OTciLCJyb2xlIjoiQ1VTVE9NRVIiLCJleHAiOjE3NjI4MTI4MDB9.example-signature")
    String accessToken,

    @Schema(
        description = "Instante UTC em que o `accessToken` expira.",
        example = "2026-05-07T20:45:00Z",
        format = "date-time")
    Instant accessTokenExpiresAt,

    @Schema(
        description = "Refresh token JWT. Use em `/api/v1/auth/refresh` para obter um novo par. Validade longa (~7 dias).",
        example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjMmI0MTIxNi0xNGY4LTRkMmMtYTBkOC0zNGI5N2RjN2I4OTciLCJ0eXBlIjoicmVmcmVzaCIsImV4cCI6MTc2MjkwMDAwMH0.example-signature")
    String refreshToken,

    @Schema(
        description = "Instante UTC em que o `refreshToken` expira.",
        example = "2026-05-14T20:30:00Z",
        format = "date-time")
    Instant refreshTokenExpiresAt
) {
    public static TokenPairResponse from(TokenPair t) {
        return new TokenPairResponse(t.accessToken(), t.accessTokenExpiresAt(),
            t.refreshToken(), t.refreshTokenExpiresAt());
    }
}
