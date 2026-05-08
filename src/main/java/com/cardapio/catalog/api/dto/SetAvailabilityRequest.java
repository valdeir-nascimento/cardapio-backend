package com.cardapio.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Toggle de disponibilidade do produto. Use para ocultar temporariamente itens em falta.")
public record SetAvailabilityRequest(

    @Schema(description = "true = produto pode ser pedido; false = exibido como indisponível.",
        example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean available
) {}
