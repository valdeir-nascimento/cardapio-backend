package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.TableView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Mesa do salão.")
public record TableResponse(

    @Schema(format = "uuid", example = "f12a3c8e-1b4d-4e9c-90b1-aa7b3e2c45ab") UUID id,

    @Schema(description = "Número físico da mesa.", example = "12") int number,

    @Schema(description = "Token público codificado no QR Code. Permite ao cliente identificar a mesa.",
        format = "uuid", example = "8e7c2f44-5b1a-49d1-ae9c-3a18d0b62e44")
    UUID qrToken,

    @Schema(description = "Mesa em uso? `false` desativa para novos pedidos.", example = "true")
    boolean active,

    @Schema(description = "Já tem comanda aberta nesta mesa.", example = "false")
    boolean hasOpenComanda
) {
    public static TableResponse from(TableView v) {
        return new TableResponse(v.id().value(), v.number(), v.qrToken(), v.active(), v.hasOpenComanda());
    }
}
