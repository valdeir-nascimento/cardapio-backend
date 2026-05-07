package com.cardapio.review.infrastructure.persistence;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.review.domain.model.Comment;
import com.cardapio.review.domain.model.Rating;
import com.cardapio.review.domain.model.Review;
import com.cardapio.review.domain.model.ReviewableOrder;
import com.cardapio.review.domain.port.ReviewRepository;
import com.cardapio.review.domain.port.ReviewableOrderRepository;
import com.cardapio.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class ReviewPersistenceIT {

    @Autowired ReviewRepository reviews;
    @Autowired ReviewableOrderRepository reviewableOrders;

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void reviewRoundTripWithoutComment() {
        OrderId orderId = OrderId.newId();
        UUID customerId = UUID.randomUUID();
        Review review = Review.create(orderId, customerId, Rating.of(5), Comment.empty(), clock);
        reviews.save(review);

        var loaded = reviews.findByOrderIdAndCustomerId(orderId, customerId).orElseThrow();
        assertThat(loaded.id()).isEqualTo(review.id());
        assertThat(loaded.rating().stars()).isEqualTo(5);
        assertThat(loaded.comment().isEmpty()).isTrue();
    }

    @Test
    void reviewRoundTripWithComment() {
        OrderId orderId = OrderId.newId();
        UUID customerId = UUID.randomUUID();
        reviews.save(Review.create(orderId, customerId, Rating.of(4), Comment.of("solid"), clock));

        var loaded = reviews.findByOrderIdAndCustomerId(orderId, customerId).orElseThrow();
        assertThat(loaded.comment().value()).isEqualTo("solid");
    }

    @Test
    void duplicateReviewBlockedByUniqueIndex() {
        OrderId orderId = OrderId.newId();
        UUID customerId = UUID.randomUUID();
        reviews.save(Review.create(orderId, customerId, Rating.of(5), Comment.empty(), clock));

        Review duplicate = Review.create(orderId, customerId, Rating.of(3), Comment.of("changed mind"), clock);
        assertThatThrownBy(() -> reviews.save(duplicate))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reviewableOrderProjectionRoundTripPreservesProductOrder() {
        OrderId orderId = OrderId.newId();
        UUID customerId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();

        reviewableOrders.save(ReviewableOrder.materialize(orderId, customerId, OrderModality.PICKUP,
            List.of(p1, p2, p3), Instant.parse("2026-05-06T13:00:00Z")));

        var loaded = reviewableOrders.findByOrderId(orderId).orElseThrow();
        assertThat(loaded.productIds()).containsExactly(p1, p2, p3);
        assertThat(loaded.modality()).isEqualTo(OrderModality.PICKUP);
    }

    @Test
    void findPendingExcludesAlreadyReviewed() {
        UUID customerId = UUID.randomUUID();

        OrderId reviewed = OrderId.newId();
        OrderId pending = OrderId.newId();
        reviewableOrders.save(ReviewableOrder.materialize(reviewed, customerId, OrderModality.PICKUP,
            List.of(UUID.randomUUID()), clock.instant()));
        reviewableOrders.save(ReviewableOrder.materialize(pending, customerId, OrderModality.PICKUP,
            List.of(UUID.randomUUID()), clock.instant()));

        reviews.save(Review.create(reviewed, customerId, Rating.of(5), Comment.empty(), clock));

        List<ReviewableOrder> pendingList = reviewableOrders.findPendingByCustomerId(customerId, 50, 0);
        assertThat(pendingList).extracting(ReviewableOrder::orderId).contains(pending).doesNotContain(reviewed);
    }
}
