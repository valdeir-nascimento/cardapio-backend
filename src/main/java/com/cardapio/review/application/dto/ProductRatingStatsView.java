package com.cardapio.review.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRatingStatsView(UUID productId, BigDecimal averageRating, long reviewCount) {}
