package com.cardapio.ordering.application.dto;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PlacedOrderView(
    OrderId id,
    OrderStatus status,
    OrderModality modality,
    BigDecimal total,
    String currency,
    Instant placedAt
) {}
