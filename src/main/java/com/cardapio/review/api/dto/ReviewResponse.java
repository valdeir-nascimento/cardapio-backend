package com.cardapio.review.api.dto;

import com.cardapio.review.application.dto.ReviewView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Avaliação registrada para um pedido.")
public record ReviewResponse(

    @Schema(format = "uuid", example = "33445566-7788-9999-aaaa-bbbbccccdddd") UUID id,

    @Schema(format = "uuid", example = "21a98176-44df-49eb-a37c-b8329f1e0123") UUID orderId,

    @Schema(format = "uuid", example = "c2b41216-14f8-4d2c-a0d8-34b97dc7b897") UUID customerId,

    @Schema(description = "1-5 estrelas.", example = "5") int rating,

    @Schema(example = "Pizza chegou quentinha e muito saborosa!", nullable = true) String comment,

    @Schema(format = "date-time", example = "2026-05-08T19:00:00Z") Instant createdAt
) {
    public static ReviewResponse from(ReviewView v) {
        return new ReviewResponse(v.id().value(), v.orderId().value(), v.customerId(),
            v.rating(), v.comment(), v.createdAt());
    }
}
