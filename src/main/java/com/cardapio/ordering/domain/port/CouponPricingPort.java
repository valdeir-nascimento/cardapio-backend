package com.cardapio.ordering.domain.port;

import com.cardapio.ordering.application.dto.CouponPricing;
import com.cardapio.shared.domain.Money;

/**
 * Read port owned by ordering and implemented by promotion via an adapter.
 * Dependency inversion: ordering depends on its own port + DTO, promotion
 * adapts its CouponQueryPort to it.
 */
public interface CouponPricingPort {
    CouponPricing evaluate(String code, Money subtotal);
}
