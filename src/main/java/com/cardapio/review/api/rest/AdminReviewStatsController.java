package com.cardapio.review.api.rest;

import com.cardapio.review.api.dto.ProductRatingStatsResponse;
import com.cardapio.review.application.usecase.ProductRatingStatsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class AdminReviewStatsController implements AdminReviewStatsApi {

    private final ProductRatingStatsUseCase stats;

    @Override
    @GetMapping("/stats")
    public List<ProductRatingStatsResponse> productStats(
        @RequestParam(required = false) UUID productId,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        return stats.execute(productId, from, to, limit, offset).stream()
            .map(ProductRatingStatsResponse::from).toList();
    }
}
