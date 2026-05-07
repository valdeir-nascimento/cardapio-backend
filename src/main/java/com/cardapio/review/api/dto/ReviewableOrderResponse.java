package com.cardapio.review.api.dto;

import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.review.application.dto.ReviewableOrderView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewableOrderResponse(
    UUID orderId,
    OrderModality modality,
    List<UUID> productIds,
    Instant terminalAt
) {
    public static ReviewableOrderResponse from(ReviewableOrderView v) {
        return new ReviewableOrderResponse(v.orderId().value(), v.modality(), v.productIds(), v.terminalAt());
    }
}
