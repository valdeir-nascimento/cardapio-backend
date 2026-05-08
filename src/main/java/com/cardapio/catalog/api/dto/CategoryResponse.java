package com.cardapio.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Categoria retornada pelo painel administrativo.")
public record CategoryResponse(

    @Schema(description = "ID interno (UUID v4).",
        example = "9c5f50d2-7a02-4c41-bb9c-d56b9c92ec0a", format = "uuid")
    UUID id,

    @Schema(description = "Nome da categoria.", example = "Pizzas Tradicionais")
    String name,

    @Schema(description = "Posição de exibição.", example = "10")
    int displayOrder,

    @Schema(description = "Se a categoria está visível no menu público.", example = "true")
    boolean active
) {}
