package com.cardapio.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Variação de tamanho/formato do produto (ex: pequena, média, grande).")
public record VariationRequest(

    @Schema(description = "Nome da variação.", example = "Tamanho Grande (12 fatias)",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String name,

    @Schema(description = "Diferença em reais somada ao `basePrice` quando esta variação for escolhida. Pode ser negativa para variações menores.",
        example = "15.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull BigDecimal priceModifier
) {}
