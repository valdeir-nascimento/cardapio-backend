package com.cardapio.review.domain.model;

import com.cardapio.ordering.domain.model.OrderId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void createCapturesAllFields() {
        OrderId orderId = OrderId.newId();
        UUID customerId = UUID.randomUUID();
        Review review = Review.create(orderId, customerId, Rating.of(4), Comment.of("muito bom"), clock);

        assertThat(review.id()).isNotNull();
        assertThat(review.orderId()).isEqualTo(orderId);
        assertThat(review.customerId()).isEqualTo(customerId);
        assertThat(review.rating().stars()).isEqualTo(4);
        assertThat(review.comment().value()).isEqualTo("muito bom");
        assertThat(review.createdAt()).isEqualTo(clock.instant());
    }

    @Test
    void emptyCommentAllowed() {
        Review review = Review.create(OrderId.newId(), UUID.randomUUID(), Rating.of(5), Comment.empty(), clock);
        assertThat(review.comment().isEmpty()).isTrue();
    }
}
