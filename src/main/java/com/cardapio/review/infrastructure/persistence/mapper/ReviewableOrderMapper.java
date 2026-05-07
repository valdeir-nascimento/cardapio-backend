package com.cardapio.review.infrastructure.persistence.mapper;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.review.domain.model.ReviewableOrder;
import com.cardapio.review.infrastructure.persistence.jpa.ReviewableOrderJpaEntity;

import java.time.Clock;

public final class ReviewableOrderMapper {

    private ReviewableOrderMapper() {}

    public static ReviewableOrderJpaEntity toJpa(ReviewableOrder reviewable, Clock clock) {
        return new ReviewableOrderJpaEntity(
            reviewable.orderId().value(),
            reviewable.customerId(),
            reviewable.modality().name(),
            reviewable.terminalAt(),
            clock.instant(),
            reviewable.productIds()
        );
    }

    public static ReviewableOrder toDomain(ReviewableOrderJpaEntity e) {
        return ReviewableOrder.materialize(
            OrderId.of(e.getOrderId()),
            e.getCustomerId(),
            OrderModality.valueOf(e.getModality()),
            e.getProductIds(),
            e.getTerminalAt()
        );
    }
}
