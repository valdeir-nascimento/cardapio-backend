package com.cardapio.promotion.application.usecase;

import com.cardapio.promotion.application.CouponQueryPort;
import com.cardapio.promotion.domain.dto.CouponEvaluation;
import com.cardapio.promotion.domain.model.Coupon;
import com.cardapio.promotion.domain.model.CouponCode;
import com.cardapio.promotion.domain.port.CouponRepository;
import com.cardapio.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EvaluateCouponUseCase implements CouponQueryPort {

    private final CouponRepository coupons;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public CouponEvaluation evaluate(CouponCode code, Money subtotal) {
        Optional<Coupon> maybe = coupons.findByCode(code);
        if (maybe.isEmpty()) return new CouponEvaluation.NotFound(code);
        Coupon coupon = maybe.get();
        if (!coupon.isActive()) return new CouponEvaluation.Inactive(code);
        if (!coupon.withinDateWindow(clock.instant())) return new CouponEvaluation.Expired(code);
        if (coupon.isExhausted()) return new CouponEvaluation.Exhausted(code);
        if (!coupon.meetsMinOrder(subtotal)) {
            return new CouponEvaluation.BelowMinOrder(code,
                coupon.minOrderValue().orElseThrow(), subtotal);
        }
        return new CouponEvaluation.Applicable(code, coupon.discountFor(subtotal), subtotal);
    }
}
