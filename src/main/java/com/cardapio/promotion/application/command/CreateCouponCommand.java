package com.cardapio.promotion.application.command;

import com.cardapio.promotion.domain.model.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCouponCommand(
    String code,
    DiscountType type,
    BigDecimal value,
    String currency,
    Instant validFrom,
    Instant validUntil,
    BigDecimal minOrderValue,
    Integer maxUses
) {}
