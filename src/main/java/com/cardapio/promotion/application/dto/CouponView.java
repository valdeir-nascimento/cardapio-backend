package com.cardapio.promotion.application.dto;

import com.cardapio.promotion.domain.model.Coupon;
import com.cardapio.promotion.domain.model.CouponId;
import com.cardapio.promotion.domain.model.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponView(
    CouponId id,
    String code,
    DiscountType type,
    BigDecimal value,
    String currency,
    Instant validFrom,
    Instant validUntil,
    BigDecimal minOrderValue,
    Integer maxUses,
    int usesCount,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static CouponView from(Coupon c) {
        return new CouponView(
            c.id(),
            c.code().value(),
            c.type(),
            c.value(),
            c.currency().getCurrencyCode(),
            c.validFrom().orElse(null),
            c.validUntil().orElse(null),
            c.minOrderValue().map(m -> m.amount()).orElse(null),
            c.maxUses().orElse(null),
            c.usesCount(),
            c.isActive(),
            c.createdAt(),
            c.updatedAt());
    }
}
