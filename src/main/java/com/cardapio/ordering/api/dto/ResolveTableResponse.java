package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.ResolveTableView;
import com.cardapio.ordering.domain.model.ComandaId;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resolve um token QR para a mesa correspondente, indicando se já há comanda aberta.")
public record ResolveTableResponse(

    @Schema(format = "uuid", example = "f12a3c8e-1b4d-4e9c-90b1-aa7b3e2c45ab") UUID tableId,

    @Schema(description = "Número físico da mesa.", example = "12") int number,

    @Schema(description = "ID da comanda aberta na mesa, se houver. `null` = abrir nova comanda.",
        format = "uuid", example = "07e2b8d8-cc88-4d4d-9311-ec0a0c6a2af0", nullable = true)
    UUID currentComandaId
) {
    public static ResolveTableResponse from(ResolveTableView v) {
        return new ResolveTableResponse(
            v.tableId().value(),
            v.number(),
            v.currentComandaId().map(ComandaId::value).orElse(null));
    }
}
