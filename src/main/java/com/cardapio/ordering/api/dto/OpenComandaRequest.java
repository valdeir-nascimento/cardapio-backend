package com.cardapio.ordering.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Abre uma nova comanda em uma mesa. O cliente que abre é o primeiro membro.")
public record OpenComandaRequest(

    @Schema(description = "ID da mesa onde a comanda será aberta. Obtido em `/api/v1/tables/resolve?token=...`.",
        example = "f12a3c8e-1b4d-4e9c-90b1-aa7b3e2c45ab", format = "uuid",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull UUID tableId
) {}
