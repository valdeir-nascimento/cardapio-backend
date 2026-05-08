package com.cardapio.delivery.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Cadastro/edição de bairro de entrega com taxa fixa.")
public record NeighborhoodRequest(

    @Schema(description = "Nome do bairro.", example = "Vila Mariana", maxLength = 120,
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank @Size(max = 120) String name,

    @Schema(description = "Cidade.", example = "São Paulo", maxLength = 120,
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank @Size(max = 120) String city,

    @Schema(description = "Taxa de entrega fixa em reais.", example = "10.00", minimum = "0.00",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull @DecimalMin("0.00") BigDecimal fee,

    @Schema(description = "Se a entrega está ativa. Default: true.", example = "true", nullable = true)
    Boolean active
) {}
