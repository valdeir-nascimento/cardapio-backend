package com.cardapio.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Dados para criar ou atualizar uma categoria de menu.")
public record CategoryRequest(

    @Schema(description = "Nome da categoria.", example = "Pizzas Tradicionais",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String name,

    @Schema(description = "Posição de exibição (menor = aparece primeiro).", example = "10",
        minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    @PositiveOrZero int displayOrder,

    @Schema(description = "Categoria visível no menu público. Default: true se omitido.",
        example = "true", nullable = true)
    Boolean active
) {}
