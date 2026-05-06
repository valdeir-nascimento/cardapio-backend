package com.cardapio.notification.domain.port;

import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface OrderSummaryPort {

    record OrderSummary(
        UUID orderId,
        UUID customerId,
        String shortRef,
        OrderModality modality,
        OrderStatus status,
        BigDecimal total
    ) {}

    Optional<OrderSummary> find(UUID orderId);
}
