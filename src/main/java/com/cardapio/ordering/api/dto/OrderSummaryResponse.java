package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.OrderSummaryView;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Resumo do pedido para listagens. Para itens detalhados use `GET /api/v1/orders/{id}`.")
public record OrderSummaryResponse(
    @Schema(format = "uuid", example = "21a98176-44df-49eb-a37c-b8329f1e0123") UUID id,
    @Schema(format = "uuid", example = "c2b41216-14f8-4d2c-a0d8-34b97dc7b897") UUID customerId,
    @Schema(example = "DELIVERY") OrderModality modality,
    @Schema(example = "PREPARING") OrderStatus status,
    @Schema(example = "98.40") BigDecimal total,
    @Schema(example = "BRL") String currency,
    @Schema(format = "date-time", example = "2026-05-07T20:30:00Z") Instant placedAt
) {
    public static OrderSummaryResponse from(OrderSummaryView v) {
        return new OrderSummaryResponse(v.id(), v.customerId(), v.modality(), v.status(),
            v.total(), v.currency(), v.placedAt());
    }
}
