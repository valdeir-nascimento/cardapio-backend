package com.cardapio.review.application.dto;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.review.domain.model.ReviewableOrder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewableOrderView(
    OrderId orderId,
    UUID customerId,
    OrderModality modality,
    List<UUID> productIds,
    Instant terminalAt
) {
    public static ReviewableOrderView from(ReviewableOrder r) {
        return new ReviewableOrderView(r.orderId(), r.customerId(), r.modality(),
            r.productIds(), r.terminalAt());
    }
}
