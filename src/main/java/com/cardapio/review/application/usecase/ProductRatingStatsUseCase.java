package com.cardapio.review.application.usecase;

import com.cardapio.review.application.dto.ProductRatingStatsView;
import com.cardapio.review.domain.port.ProductRatingStatsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductRatingStatsUseCase {

    private final ProductRatingStatsPort port;

    @Transactional(readOnly = true)
    public List<ProductRatingStatsView> execute(UUID productId, Instant from, Instant to, int limit, int offset) {
        return port.find(productId, from, to, limit, offset).stream()
            .map(row -> new ProductRatingStatsView(
                row.productId(),
                row.averageRating().setScale(2, RoundingMode.HALF_EVEN),
                row.reviewCount()))
            .toList();
    }
}
