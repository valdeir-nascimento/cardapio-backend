package com.cardapio.promotion.application.event;

import com.cardapio.ordering.domain.event.OrderCanceled;
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
class OrderCanceledCouponListener {

    private final CouponRepository coupons;
    private final CouponUseRepository couponUses;
    private final Clock clock;

    @ApplicationModuleListener
    void on(OrderCanceled event) {
        if (event.couponCode() == null || event.couponCode().isBlank()) return;

        CouponCode code;
        try {
            code = CouponCode.of(event.couponCode());
        } catch (IllegalArgumentException e) {
            log.warn("OrderCanceled carried malformed coupon code '{}' (orderId={}); skipping",
                event.couponCode(), event.orderId().value());
            return;
        }

        if (!couponUses.exists(code, event.orderId().value())) {
            log.debug("OrderCanceled for coupon {} order {} but no use ledger entry; skipping",
                code.value(), event.orderId().value());
            return;
        }

        Coupon coupon = coupons.findByCode(code).orElse(null);
        if (coupon == null) {
            log.warn("OrderCanceled references missing coupon {} (orderId={})",
                code.value(), event.orderId().value());
            // Still purge the ledger row so a future place won't be blocked by a stale entry.
            couponUses.remove(code, event.orderId().value());
            return;
        }

        coupon.decrementUses(clock);
        coupons.save(coupon);
        couponUses.remove(code, event.orderId().value());
    }
}
