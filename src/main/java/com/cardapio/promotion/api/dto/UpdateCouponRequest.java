package com.cardapio.promotion.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateCouponRequest(
    @DecimalMin(value = "0.01") BigDecimal value,
    Instant validUntil,
    @DecimalMin(value = "0.00") BigDecimal minOrderValue,
    @Min(0) Integer maxUses
) {}
