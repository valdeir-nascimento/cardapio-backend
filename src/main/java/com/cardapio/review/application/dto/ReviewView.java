package com.cardapio.review.application.dto;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.review.domain.model.Review;
import com.cardapio.review.domain.model.ReviewId;

import java.time.Instant;
import java.util.UUID;

public record ReviewView(
    ReviewId id,
    OrderId orderId,
    UUID customerId,
    int rating,
    String comment,
    Instant createdAt
) {
    public static ReviewView from(Review review) {
        return new ReviewView(review.id(), review.orderId(), review.customerId(),
            review.rating().stars(), review.comment().value(), review.createdAt());
    }
}
