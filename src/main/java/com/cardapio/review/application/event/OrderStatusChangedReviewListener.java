package com.cardapio.review.application.event;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.ordering.application.OrderingFacade;
import com.cardapio.ordering.application.dto.OrderItemView;
import com.cardapio.ordering.application.dto.OrderView;
import com.cardapio.ordering.domain.event.OrderStatusChanged;
import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.review.domain.model.ReviewableOrder;
import com.cardapio.review.domain.port.ReviewableOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
class OrderStatusChangedReviewListener {

    private final OrderingFacade ordering;
    private final ReviewableOrderRepository reviewableOrders;

    @ApplicationModuleListener
    void on(OrderStatusChanged event) {
        OrderStatus to = event.to();
        switch (to) {
            case DELIVERED, PICKED_UP, SERVED -> materializeProjection(event);
            case CANCELED -> reviewableOrders.deleteByOrderId(event.orderId());
            default -> {} // no-op for non-terminal transitions
        }
    }

    private void materializeProjection(OrderStatusChanged event) {
        OrderView order;
        try {
            order = ordering.getOrderAdmin(event.orderId());
        } catch (NotFoundException e) {
            log.warn("OrderStatusChanged({}) but order is missing; skipping projection",
                event.orderId().value());
            return;
        }
        List<UUID> productIds = order.items().stream()
            .map(OrderItemView::productId)
            .distinct()
            .toList();
        ReviewableOrder reviewable = ReviewableOrder.materialize(
            event.orderId(),
            order.customerId(),
            order.modality(),
            productIds,
            event.occurredOn());
        reviewableOrders.save(reviewable);
    }
}
