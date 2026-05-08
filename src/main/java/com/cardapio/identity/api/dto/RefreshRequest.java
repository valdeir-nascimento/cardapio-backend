package com.cardapio.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Pedido para renovar o par de tokens (access + refresh) sem precisar autenticar de novo.")
public record RefreshRequest(

    @Schema(
        description = "Refresh token recebido no último login. Válido até `refreshTokenExpiresAt`.",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjMmI0MTIxNi0xNGY4LTRkMmMtYTBkOC0zNGI5N2RjN2I4OTciLCJ0eXBlIjoicmVmcmVzaCIsImV4cCI6MTc2MjkwMDAwMH0.example-signature",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String refreshToken
) {}
