package com.cardapio.review.api.dto;

import com.cardapio.review.application.dto.ProductRatingStatsView;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRatingStatsResponse(UUID productId, BigDecimal averageRating, long reviewCount) {
    public static ProductRatingStatsResponse from(ProductRatingStatsView v) {
        return new ProductRatingStatsResponse(v.productId(), v.averageRating(), v.reviewCount());
    }
}
