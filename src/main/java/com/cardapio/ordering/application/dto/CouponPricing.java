package com.cardapio.ordering.application.dto;

import com.cardapio.shared.domain.Money;

/**
 * Read view of a coupon evaluation, owned by the ordering module so it can
 * call into promotion through dependency inversion (CouponPricingPort).
 */
public sealed interface CouponPricing {
    String code();

    record Applicable(String code, Money discount, Money subtotal) implements CouponPricing {}
    record NotFound(String code) implements CouponPricing {}
    record Inactive(String code) implements CouponPricing {}
    record Expired(String code) implements CouponPricing {}
    record BelowMinOrder(String code, Money minOrder, Money subtotal) implements CouponPricing {}
    record Exhausted(String code) implements CouponPricing {}
}
