package com.cardapio.ordering.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "Edita quantidade e observação de um item já no carrinho.")
public record UpdateCartItemRequest(

    @Schema(description = "Nova quantidade. Mínimo 1 (para remover, use DELETE).",
        example = "2", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(1) int quantity,

    @Schema(description = "Nova observação (substitui a anterior).",
        example = "Sem cebola, sem azeitona.", maxLength = 200, nullable = true)
    @Size(max = 200) String observation
) {}
