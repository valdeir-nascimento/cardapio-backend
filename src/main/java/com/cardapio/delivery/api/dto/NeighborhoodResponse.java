package com.cardapio.delivery.api.dto;

import com.cardapio.delivery.application.dto.NeighborhoodView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Bairro atendido pelo delivery com sua taxa.")
public record NeighborhoodResponse(

    @Schema(format = "uuid", example = "8b4e3d2a-c1f5-4321-9876-fedcba012345") UUID id,

    @Schema(example = "Vila Mariana") String name,

    @Schema(example = "São Paulo") String city,

    @Schema(description = "Taxa em reais.", example = "10.00") BigDecimal fee,

    @Schema(example = "BRL") String currency,

    @Schema(description = "Se está sendo entregue.", example = "true") boolean active
) {
    public static NeighborhoodResponse from(NeighborhoodView v) {
        return new NeighborhoodResponse(v.id(), v.name(), v.city(), v.fee(), v.currency(), v.active());
    }
}
