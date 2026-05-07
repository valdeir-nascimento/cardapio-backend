package com.cardapio.review.infrastructure.persistence.jpa;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reviewable_orders")
public class ReviewableOrderJpaEntity {

    @Id @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(nullable = false, length = 16) private String modality;
    @Column(name = "terminal_at", nullable = false) private Instant terminalAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reviewable_order_products", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "position")
    @Column(name = "product_id", nullable = false)
    private List<UUID> productIds = new ArrayList<>();

    protected ReviewableOrderJpaEntity() {}

    public ReviewableOrderJpaEntity(UUID orderId, UUID customerId, String modality,
                                    Instant terminalAt, Instant createdAt, List<UUID> productIds) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.modality = modality;
        this.terminalAt = terminalAt;
        this.createdAt = createdAt;
        this.productIds = new ArrayList<>(productIds);
    }

    public UUID getOrderId() { return orderId; }
    public UUID getCustomerId() { return customerId; }
    public String getModality() { return modality; }
    public Instant getTerminalAt() { return terminalAt; }
    public Instant getCreatedAt() { return createdAt; }
    public List<UUID> getProductIds() { return productIds; }
}
