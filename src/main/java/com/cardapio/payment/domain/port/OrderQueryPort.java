package com.cardapio.payment.domain.port;

import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.shared.domain.Money;

import java.util.Optional;
import java.util.UUID;

public interface OrderQueryPort {

    Optional<OrderSnapshot> loadOrder(UUID orderId);

    record OrderSnapshot(
        UUID orderId,
        UUID customerId,
        Money total,
        OrderStatus status,
        OrderModality modality
    ) {}
}
