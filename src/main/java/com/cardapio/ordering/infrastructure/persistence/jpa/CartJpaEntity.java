package com.cardapio.ordering.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class CartJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "customer_id", nullable = false, unique = true) private UUID customerId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @OneToMany(mappedBy = "cartId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<CartItemJpaEntity> items = new ArrayList<>();

    protected CartJpaEntity() {}

    public CartJpaEntity(UUID id, UUID customerId, Instant createdAt, Instant updatedAt) {
        this.id = id; this.customerId = customerId; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<CartItemJpaEntity> getItems() { return items; }

    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
