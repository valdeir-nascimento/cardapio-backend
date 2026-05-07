package com.cardapio.review.infrastructure.persistence.mapper;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.review.domain.model.Comment;
import com.cardapio.review.domain.model.Rating;
import com.cardapio.review.domain.model.Review;
import com.cardapio.review.domain.model.ReviewId;
import com.cardapio.review.infrastructure.persistence.jpa.ReviewJpaEntity;

public final class ReviewMapper {

    private ReviewMapper() {}

    public static ReviewJpaEntity toJpa(Review review) {
        return new ReviewJpaEntity(
            review.id().value(),
            review.orderId().value(),
            review.customerId(),
            review.rating().stars(),
            review.comment().value(),
            review.createdAt()
        );
    }

    public static Review toDomain(ReviewJpaEntity e) {
        return Review.rehydrate(
            ReviewId.of(e.getId()),
            OrderId.of(e.getOrderId()),
            e.getCustomerId(),
            Rating.of(e.getRating()),
            Comment.of(e.getComment()),
            e.getCreatedAt()
        );
    }
}
