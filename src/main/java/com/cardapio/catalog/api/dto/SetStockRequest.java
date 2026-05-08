package com.cardapio.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Atualiza o estoque de um produto.")
public record SetStockRequest(

    @Schema(description = "Quantidade disponível em estoque. `null` significa estoque ilimitado/não controlado.",
        example = "20", nullable = true, minimum = "0")
    Integer quantity
) {}
