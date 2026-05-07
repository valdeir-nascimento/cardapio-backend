package com.cardapio.review.domain.port;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.review.domain.model.ReviewableOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewableOrderRepository {
    void save(ReviewableOrder reviewable);
    void deleteByOrderId(OrderId orderId);
    Optional<ReviewableOrder> findByOrderId(OrderId orderId);
    List<ReviewableOrder> findPendingByCustomerId(UUID customerId, int limit, int offset);
}
