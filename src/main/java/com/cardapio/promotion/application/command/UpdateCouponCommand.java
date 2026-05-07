package com.cardapio.promotion.application.command;

import com.cardapio.promotion.domain.model.CouponId;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateCouponCommand(
    CouponId id,
    BigDecimal value,
    Instant validUntil,
    BigDecimal minOrderValue,
    Integer maxUses
) {}
