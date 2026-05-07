package com.cardapio.promotion.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupons")
public class CouponJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(nullable = false, length = 32) private String code;
    @Column(name = "discount_type", nullable = false, length = 10) private String discountType;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal value;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "valid_from") private Instant validFrom;
    @Column(name = "valid_until") private Instant validUntil;
    @Column(name = "min_order_value", precision = 12, scale = 2) private BigDecimal minOrderValue;
    @Column(name = "max_uses") private Integer maxUses;
    @Column(name = "uses_count", nullable = false) private Integer usesCount;
    @Column(nullable = false) private Boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected CouponJpaEntity() {}

    public CouponJpaEntity(UUID id, String code, String discountType, BigDecimal value, String currency,
                           Instant validFrom, Instant validUntil, BigDecimal minOrderValue, Integer maxUses,
                           Integer usesCount, Boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.code = code;
        this.discountType = discountType;
        this.value = value;
        this.currency = currency;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.minOrderValue = minOrderValue;
        this.maxUses = maxUses;
        this.usesCount = usesCount;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getDiscountType() { return discountType; }
    public BigDecimal getValue() { return value; }
    public String getCurrency() { return currency; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public BigDecimal getMinOrderValue() { return minOrderValue; }
    public Integer getMaxUses() { return maxUses; }
    public Integer getUsesCount() { return usesCount; }
    public Boolean getActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setValue(BigDecimal value) { this.value = value; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }
    public void setMinOrderValue(BigDecimal minOrderValue) { this.minOrderValue = minOrderValue; }
    public void setMaxUses(Integer maxUses) { this.maxUses = maxUses; }
    public void setUsesCount(Integer usesCount) { this.usesCount = usesCount; }
    public void setActive(Boolean active) { this.active = active; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
