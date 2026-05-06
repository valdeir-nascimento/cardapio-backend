package com.cardapio.ordering.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyJpaEntity {

    @EmbeddedId
    private IdempotencyKeyId id;

    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected IdempotencyKeyJpaEntity() {}

    public IdempotencyKeyJpaEntity(IdempotencyKeyId id, UUID orderId, Instant createdAt) {
        this.id = id; this.orderId = orderId; this.createdAt = createdAt;
    }

    public IdempotencyKeyId getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public Instant getCreatedAt() { return createdAt; }

    @Embeddable
    public static class IdempotencyKeyId implements Serializable {
        @Column(name = "customer_id", nullable = false) private UUID customerId;
        @Column(name = "key", nullable = false, length = 120) private String key;

        protected IdempotencyKeyId() {}

        public IdempotencyKeyId(UUID customerId, String key) {
            this.customerId = customerId; this.key = key;
        }

        public UUID getCustomerId() { return customerId; }
        public String getKey() { return key; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IdempotencyKeyId other)) return false;
            return Objects.equals(customerId, other.customerId) && Objects.equals(key, other.key);
        }

        @Override public int hashCode() { return Objects.hash(customerId, key); }
    }
}
