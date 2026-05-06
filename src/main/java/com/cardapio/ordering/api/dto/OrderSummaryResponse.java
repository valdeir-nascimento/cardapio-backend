package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.OrderSummaryView;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
    UUID id, UUID customerId, OrderModality modality, OrderStatus status,
    BigDecimal total, String currency, Instant placedAt
) {
    public static OrderSummaryResponse from(OrderSummaryView v) {
        return new OrderSummaryResponse(v.id(), v.customerId(), v.modality(), v.status(),
            v.total(), v.currency(), v.placedAt());
    }
}
