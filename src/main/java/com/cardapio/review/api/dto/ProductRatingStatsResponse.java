package com.cardapio.review.api.dto;

import com.cardapio.review.application.dto.ProductRatingStatsView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Estatísticas agregadas de avaliações por produto.")
public record ProductRatingStatsResponse(

    @Schema(format = "uuid", example = "ad4f3c1b-7a02-4c41-bb9c-d56b9c92ec0a") UUID productId,

    @Schema(description = "Média ponderada das notas (1-5).", example = "4.7") BigDecimal averageRating,

    @Schema(description = "Quantidade total de avaliações deste produto no período.", example = "127")
    long reviewCount
) {
    public static ProductRatingStatsResponse from(ProductRatingStatsView v) {
        return new ProductRatingStatsResponse(v.productId(), v.averageRating(), v.reviewCount());
    }
}
