package com.cardapio.review.domain.model;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.shared.domain.AggregateRoot;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Review extends AggregateRoot<ReviewId> {

    private final OrderId orderId;
    private final UUID customerId;
    private final Rating rating;
    private final Comment comment;
    private final Instant createdAt;

    private Review(ReviewId id, OrderId orderId, UUID customerId, Rating rating, Comment comment, Instant createdAt) {
        super(id);
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.rating = Objects.requireNonNull(rating, "rating");
        this.comment = Objects.requireNonNull(comment, "comment");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Review create(OrderId orderId, UUID customerId, Rating rating, Comment comment, Clock clock) {
        return new Review(ReviewId.newId(), orderId, customerId, rating, comment, clock.instant());
    }

    public static Review rehydrate(ReviewId id, OrderId orderId, UUID customerId, Rating rating,
                                   Comment comment, Instant createdAt) {
        return new Review(id, orderId, customerId, rating, comment, createdAt);
    }

    public OrderId orderId() { return orderId; }
    public UUID customerId() { return customerId; }
    public Rating rating() { return rating; }
    public Comment comment() { return comment; }
    public Instant createdAt() { return createdAt; }
}
