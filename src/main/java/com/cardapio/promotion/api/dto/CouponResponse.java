package com.cardapio.promotion.api.dto;

import com.cardapio.promotion.application.dto.CouponView;
import com.cardapio.promotion.domain.model.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
    UUID id,
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
    public static CouponResponse from(CouponView v) {
        return new CouponResponse(
            v.id().value(), v.code(), v.type(), v.value(), v.currency(),
            v.validFrom(), v.validUntil(), v.minOrderValue(), v.maxUses(),
            v.usesCount(), v.active(), v.createdAt(), v.updatedAt());
    }
}
