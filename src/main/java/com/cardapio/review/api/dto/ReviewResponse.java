package com.cardapio.review.api.dto;

import com.cardapio.review.application.dto.ReviewView;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID orderId,
    UUID customerId,
    int rating,
    String comment,
    Instant createdAt
) {
    public static ReviewResponse from(ReviewView v) {
        return new ReviewResponse(v.id().value(), v.orderId().value(), v.customerId(),
            v.rating(), v.comment(), v.createdAt());
    }
}
