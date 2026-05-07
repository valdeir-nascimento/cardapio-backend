package com.cardapio.review.domain.model;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderModality;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Read-side projection materialized from OrderStatusChanged terminal-success
 * transitions. Carries enough denormalized data so the customer flow can list
 * reviewable orders without calling back into ordering at submission time.
 */
public final class ReviewableOrder {

    private final OrderId orderId;
    private final UUID customerId;
    private final OrderModality modality;
    private final List<UUID> productIds;
    private final Instant terminalAt;

    private ReviewableOrder(OrderId orderId, UUID customerId, OrderModality modality,
                            List<UUID> productIds, Instant terminalAt) {
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.modality = Objects.requireNonNull(modality, "modality");
        this.productIds = List.copyOf(Objects.requireNonNull(productIds, "productIds"));
        this.terminalAt = Objects.requireNonNull(terminalAt, "terminalAt");
    }

    public static ReviewableOrder materialize(OrderId orderId, UUID customerId, OrderModality modality,
                                              List<UUID> productIds, Instant terminalAt) {
        return new ReviewableOrder(orderId, customerId, modality, productIds, terminalAt);
    }

    public OrderId orderId() { return orderId; }
    public UUID customerId() { return customerId; }
    public OrderModality modality() { return modality; }
    public List<UUID> productIds() { return productIds; }
    public Instant terminalAt() { return terminalAt; }
}
