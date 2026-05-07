package com.cardapio.review.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Spring Data interface projection used by the native rating stats query.
 * Aliases must match the SELECT column names.
 */
public interface ProductRatingStatsRow {
    UUID getProductId();
    BigDecimal getAvgRating();
    Long getReviewCount();
}
