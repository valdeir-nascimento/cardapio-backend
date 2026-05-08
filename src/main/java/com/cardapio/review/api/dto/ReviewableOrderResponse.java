package com.cardapio.review.api.dto;

import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.review.application.dto.ReviewableOrderView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Pedido finalizado que ainda não foi avaliado pelo cliente.")
public record ReviewableOrderResponse(

    @Schema(format = "uuid", example = "21a98176-44df-49eb-a37c-b8329f1e0123") UUID orderId,

    @Schema(example = "DELIVERY") OrderModality modality,

    @Schema(description = "IDs dos produtos pedidos. Útil para exibir miniaturas no formulário de avaliação.")
    List<UUID> productIds,

    @Schema(description = "Quando o pedido entrou em estado terminal (DELIVERED ou CANCELLED).",
        format = "date-time", example = "2026-05-08T18:30:00Z")
    Instant terminalAt
) {
    public static ReviewableOrderResponse from(ReviewableOrderView v) {
        return new ReviewableOrderResponse(v.orderId().value(), v.modality(), v.productIds(), v.terminalAt());
    }
}
