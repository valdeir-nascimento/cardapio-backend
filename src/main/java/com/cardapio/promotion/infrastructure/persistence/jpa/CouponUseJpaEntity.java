package com.cardapio.promotion.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "coupon_uses")
@IdClass(CouponUseJpaEntity.PK.class)
public class CouponUseJpaEntity {

    @Id @Column(name = "coupon_code", nullable = false, length = 32) private String couponCode;
    @Id @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "applied_at", nullable = false) private Instant appliedAt;

    protected CouponUseJpaEntity() {}

    public CouponUseJpaEntity(String couponCode, UUID orderId, Instant appliedAt) {
        this.couponCode = couponCode;
        this.orderId = orderId;
        this.appliedAt = appliedAt;
    }

    public String getCouponCode() { return couponCode; }
    public UUID getOrderId() { return orderId; }
    public Instant getAppliedAt() { return appliedAt; }

    public static class PK implements Serializable {
        private String couponCode;
        private UUID orderId;

        public PK() {}
        public PK(String couponCode, UUID orderId) {
            this.couponCode = couponCode;
            this.orderId = orderId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(couponCode, pk.couponCode) && Objects.equals(orderId, pk.orderId);
        }

        @Override
        public int hashCode() { return Objects.hash(couponCode, orderId); }
    }
}
