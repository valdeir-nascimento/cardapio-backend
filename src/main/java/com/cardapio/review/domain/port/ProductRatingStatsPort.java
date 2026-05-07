package com.cardapio.review.domain.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProductRatingStatsPort {

    List<Row> find(UUID productId, Instant from, Instant to, int limit, int offset);

    record Row(UUID productId, BigDecimal averageRating, long reviewCount) {}
}
