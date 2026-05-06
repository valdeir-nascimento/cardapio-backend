package com.cardapio.promotion.application.event;

import com.cardapio.ordering.domain.event.OrderPlaced;
import com.cardapio.promotion.domain.exception.CouponInvariantException;
import com.cardapio.promotion.domain.model.Coupon;
import com.cardapio.promotion.domain.model.CouponCode;
import com.cardapio.promotion.domain.port.CouponRepository;
import com.cardapio.promotion.domain.port.CouponUseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@RequiredArgsConstructor
@Slf4j
class OrderPlacedCouponListener {

    private final CouponRepository coupons;
    private final CouponUseRepository couponUses;
    private final Clock clock;

    @ApplicationModuleListener
    void on(OrderPlaced event) {
        if (event.couponCode() == null || event.couponCode().isBlank()) return;

        CouponCode code;
        try {
            code = CouponCode.of(event.couponCode());
        } catch (IllegalArgumentException e) {
            log.warn("OrderPlaced carried malformed coupon code '{}' (orderId={}); skipping",
                event.couponCode(), event.orderId().value());
            return;
        }

        if (couponUses.exists(code, event.orderId().value())) {
            log.debug("Duplicate OrderPlaced delivery for coupon {} order {}; skipping",
                code.value(), event.orderId().value());
            return;
        }

        Coupon coupon = coupons.findByCode(code).orElse(null);
        if (coupon == null) {
            log.warn("OrderPlaced references missing coupon {} (orderId={})",
                code.value(), event.orderId().value());
            return;
        }

        try {
            coupon.incrementUses(clock);
        } catch (CouponInvariantException e) {
            log.warn("Cannot increment coupon {} for order {}: {}",
                code.value(), event.orderId().value(), e.getMessage());
            return;
        }
        coupons.save(coupon);
        couponUses.register(code, event.orderId().value(), event.occurredOn());
    }
}
