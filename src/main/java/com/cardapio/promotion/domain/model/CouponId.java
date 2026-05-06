package com.cardapio.promotion.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CouponId(UUID value) {
    public CouponId { Objects.requireNonNull(value, "value"); }
    public static CouponId newId() { return new CouponId(UUID.randomUUID()); }
    public static CouponId of(UUID value) { return new CouponId(value); }
    public static CouponId of(String value) { return new CouponId(UUID.fromString(value)); }
}
