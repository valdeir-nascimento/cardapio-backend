package com.cardapio.ordering.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Aplica um código de cupom ao carrinho.")
public record ApplyCouponRequest(

    @Schema(description = "Código do cupom (case-insensitive).",
        example = "BEMVINDO10", maxLength = 32, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank @Size(max = 32) String code
) {}
