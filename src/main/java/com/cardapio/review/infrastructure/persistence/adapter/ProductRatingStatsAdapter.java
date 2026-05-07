package com.cardapio.review.infrastructure.persistence.adapter;

import com.cardapio.review.domain.port.ProductRatingStatsPort;
import com.cardapio.review.infrastructure.persistence.repository.SpringReviewJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductRatingStatsAdapter implements ProductRatingStatsPort {

    private final SpringReviewJpaRepository jpa;

    @Override
    public List<Row> find(UUID productId, Instant from, Instant to, int limit, int offset) {
        return jpa.findProductRatingStats(productId, from, to, limit, offset).stream()
            .map(row -> new Row(row.getProductId(), row.getAvgRating(), row.getReviewCount()))
            .toList();
    }
}
