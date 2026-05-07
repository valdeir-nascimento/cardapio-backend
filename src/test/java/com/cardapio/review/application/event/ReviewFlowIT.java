package com.cardapio.review.application.event;

import com.cardapio.ordering.domain.event.OrderStatusChanged;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.review.application.command.SubmitReviewCommand;
import com.cardapio.review.application.usecase.SubmitReviewUseCase;
import com.cardapio.review.domain.model.ReviewableOrder;
import com.cardapio.review.domain.port.ReviewRepository;
import com.cardapio.review.domain.port.ReviewableOrderRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import com.cardapio.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class ReviewFlowIT {

    @Autowired ReviewableOrderRepository reviewableOrders;
    @Autowired ReviewRepository reviews;
    @Autowired SubmitReviewUseCase submitReview;
    @Autowired ApplicationEventPublisher events;
    @Autowired TransactionTemplate tx;

    private void publishInTx(Object event) {
        tx.executeWithoutResult(s -> events.publishEvent(event));
    }

    @Test
    void submitFailsWhenProjectionMissing() {
        // No prior OrderStatusChanged published — projection is empty.
        Result<?> result = submitReview.execute(new SubmitReviewCommand(
            OrderId.newId(), UUID.randomUUID(), 5, "great"));
        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<?>) result).notification().errors())
            .anyMatch(e -> ErrorCode.ORDER_NOT_REVIEWABLE.name().equals(e.code()));
    }

    @Test
    void seededProjectionAllowsSubmissionAndBlocksDuplicate() {
        OrderId orderId = OrderId.newId();
        UUID customerId = UUID.randomUUID();
        // Seed the projection directly — the listener path is exercised in
        // ReviewListenerIT (which boots a full ordering pipeline). This test
        // just isolates the submit use case + repository behavior.
        reviewableOrders.save(ReviewableOrder.materialize(orderId, customerId,
            com.cardapio.ordering.domain.model.OrderModality.PICKUP,
            List.of(UUID.randomUUID()), Instant.now()));

        var first = submitReview.execute(new SubmitReviewCommand(orderId, customerId, 4, "good"));
        assertThat(first.isSuccess()).isTrue();

        var dup = submitReview.execute(new SubmitReviewCommand(orderId, customerId, 5, "amazing"));
        assertThat(dup.isSuccess()).isFalse();
        assertThat(((Result.Failure<?>) dup).notification().errors())
            .anyMatch(e -> ErrorCode.REVIEW_ALREADY_SUBMITTED.name().equals(e.code()));
    }

    @Test
    void canceledTerminalEventClearsProjection() {
        OrderId orderId = OrderId.newId();
        UUID customerId = UUID.randomUUID();
        reviewableOrders.save(ReviewableOrder.materialize(orderId, customerId,
            com.cardapio.ordering.domain.model.OrderModality.PICKUP,
            List.of(UUID.randomUUID()), Instant.now()));

        publishInTx(OrderStatusChanged.of(orderId, OrderStatus.RECEIVED, OrderStatus.CANCELED, Instant.now()));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
            assertThat(reviewableOrders.findByOrderId(orderId)).isEmpty());
    }

    @Test
    void terminalSuccessOrderWithoutSeededOrderingResultsInNoOp() {
        // Listener tries to look up the order via OrderingFacade; missing order
        // is logged but the listener does not throw.
        OrderId orderId = OrderId.newId();
        publishInTx(OrderStatusChanged.of(orderId, OrderStatus.READY, OrderStatus.PICKED_UP, Instant.now()));

        // give the listener a beat
        try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

        assertThat(reviewableOrders.findByOrderId(orderId)).isEmpty();
        assertThat(reviews.findByOrderIdAndCustomerId(orderId, UUID.randomUUID())).isEmpty();
    }
}
