package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.ComandaSummaryView;
import com.cardapio.ordering.application.dto.ComandaView;
import com.cardapio.ordering.domain.model.ComandaStatus;
import com.cardapio.ordering.domain.model.OrderId;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Comanda compartilhada — agrupa pedidos de múltiplos clientes na mesma mesa.")
public record ComandaResponse(

    @Schema(format = "uuid", example = "07e2b8d8-cc88-4d4d-9311-ec0a0c6a2af0") UUID id,

    @Schema(format = "uuid", example = "f12a3c8e-1b4d-4e9c-90b1-aa7b3e2c45ab") UUID tableId,

    @Schema(description = "Estado da comanda.", example = "OPEN") ComandaStatus status,

    @Schema(description = "IDs dos clientes membros. Vazio em listagens (use `GET /comandas/{id}` para detalhes).")
    Set<UUID> customerIds,

    @Schema(description = "IDs dos pedidos lançados na comanda. Vazio em listagens.")
    List<UUID> orderIds,

    @Schema(format = "date-time", example = "2026-05-07T19:15:00Z") Instant openedAt,

    @Schema(description = "Quando foi fechada (null se ainda aberta).",
        format = "date-time", example = "2026-05-07T22:40:00Z", nullable = true)
    Instant closedAt
) {
    public static ComandaResponse from(ComandaView v) {
        return new ComandaResponse(
            v.id().value(),
            v.tableId().value(),
            v.status(),
            v.customerIds(),
            v.orderIds().stream().map(OrderId::value).toList(),
            v.openedAt(),
            v.closedAt());
    }

    public static ComandaResponse from(ComandaSummaryView v) {
        return new ComandaResponse(
            v.id().value(),
            v.tableId().value(),
            v.status(),
            Set.of(),
            List.of(),
            v.openedAt(),
            v.closedAt());
    }
}
