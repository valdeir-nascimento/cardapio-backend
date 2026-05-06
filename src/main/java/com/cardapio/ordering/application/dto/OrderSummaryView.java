package com.cardapio.ordering.application.dto;

import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryView(
    UUID id,
    UUID customerId,
    OrderModality modality,
    OrderStatus status,
    BigDecimal total,
    String currency,
    Instant placedAt
) {}
