package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.PlacedOrderView;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlaceOrderResponse(
    UUID id,
    OrderStatus status,
    OrderModality modality,
    BigDecimal total,
    String currency,
    Instant placedAt
) {
    public static PlaceOrderResponse from(PlacedOrderView v) {
        return new PlaceOrderResponse(v.id().value(), v.status(), v.modality(), v.total(), v.currency(), v.placedAt());
    }
}
