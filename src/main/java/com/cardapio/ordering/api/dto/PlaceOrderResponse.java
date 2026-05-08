package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.PlacedOrderView;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Resumo do pedido recém-criado. Use o `id` para iniciar o pagamento.")
public record PlaceOrderResponse(

    @Schema(description = "ID do pedido criado.",
        example = "21a98176-44df-49eb-a37c-b8329f1e0123", format = "uuid")
    UUID id,

    @Schema(description = "Status inicial — geralmente PLACED até o pagamento ser confirmado.",
        example = "PLACED")
    OrderStatus status,

    @Schema(description = "Modalidade escolhida.", example = "DELIVERY")
    OrderModality modality,

    @Schema(description = "Valor total cobrado em reais (subtotal + delivery - desconto).", example = "98.40")
    BigDecimal total,

    @Schema(example = "BRL")
    String currency,

    @Schema(description = "Quando o pedido foi recebido pelo backend (UTC).",
        example = "2026-05-07T20:30:00Z", format = "date-time")
    Instant placedAt
) {
    public static PlaceOrderResponse from(PlacedOrderView v) {
        return new PlaceOrderResponse(v.id().value(), v.status(), v.modality(), v.total(), v.currency(), v.placedAt());
    }
}
