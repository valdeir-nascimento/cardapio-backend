package com.cardapio.ordering.domain.port;

import com.cardapio.ordering.domain.model.Order;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId id);
    List<Order> findByCustomerId(UUID customerId, int limit, int offset);

    /**
     * @param status optional filter (null = all)
     * @param from   optional placedAt lower bound (null = no bound)
     * @param to     optional placedAt upper bound (null = no bound)
     */
    List<Order> findAdmin(OrderStatus status, Instant from, Instant to, int limit, int offset);

    long countAdmin(OrderStatus status, Instant from, Instant to);
}
