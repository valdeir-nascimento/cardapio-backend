package com.cardapio.ordering.domain.port;

import com.cardapio.ordering.domain.model.OrderId;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyStore {

    Optional<OrderId> findExisting(String key, UUID customerId);

    void register(String key, UUID customerId, OrderId orderId);
}
