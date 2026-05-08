package com.cardapio.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Item individual de um grupo de adicionais.")
public record AddOnItemRequest(

    @Schema(description = "Nome do item.", example = "Borda de Catupiry",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String name,

    @Schema(description = "Preço extra em reais (BRL) cobrado quando o item é selecionado.",
        example = "8.50", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull BigDecimal price
) {}
