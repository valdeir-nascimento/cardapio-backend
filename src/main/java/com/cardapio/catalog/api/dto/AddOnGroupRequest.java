package com.cardapio.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

@Schema(description = "Grupo de adicionais opcionais (ex: 'Bordas', 'Acompanhamentos').")
public record AddOnGroupRequest(

    @Schema(description = "Nome do grupo.", example = "Bordas Recheadas",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String name,

    @Schema(description = "Mínimo de itens que o cliente deve escolher (0 = opcional).",
        example = "0", minimum = "0")
    @PositiveOrZero int minSelection,

    @Schema(description = "Máximo de itens que o cliente pode escolher.",
        example = "1", minimum = "0")
    @PositiveOrZero int maxSelection,

    @Schema(description = "Itens disponíveis dentro deste grupo.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull @Valid List<AddOnItemRequest> items
) {}
