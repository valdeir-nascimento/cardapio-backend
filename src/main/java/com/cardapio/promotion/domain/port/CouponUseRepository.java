package com.cardapio.promotion.domain.port;

import com.cardapio.promotion.domain.model.CouponCode;

import java.time.Instant;
import java.util.UUID;

public interface CouponUseRepository {
    boolean exists(CouponCode code, UUID orderId);
    void register(CouponCode code, UUID orderId, Instant appliedAt);
    void remove(CouponCode code, UUID orderId);
}
