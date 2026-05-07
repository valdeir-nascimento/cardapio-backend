package com.cardapio.review.domain.port;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.review.domain.model.Review;
import com.cardapio.review.domain.model.ReviewId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository {
    void save(Review review);
    Optional<Review> findById(ReviewId id);
    Optional<Review> findByOrderIdAndCustomerId(OrderId orderId, UUID customerId);
    boolean existsByOrderIdAndCustomerId(OrderId orderId, UUID customerId);
    List<Review> findAllByCustomerId(UUID customerId, int limit, int offset);
}
