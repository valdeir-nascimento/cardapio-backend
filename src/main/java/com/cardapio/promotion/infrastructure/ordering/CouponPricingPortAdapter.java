package com.cardapio.promotion.infrastructure.ordering;

import com.cardapio.ordering.application.dto.CouponPricing;
import com.cardapio.ordering.domain.port.CouponPricingPort;
import com.cardapio.promotion.application.CouponQueryPort;
import com.cardapio.promotion.domain.dto.CouponEvaluation;
import com.cardapio.promotion.domain.model.CouponCode;
import com.cardapio.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponPricingPortAdapter implements CouponPricingPort {

    private final CouponQueryPort couponQuery;

    @Override
    public CouponPricing evaluate(String rawCode, Money subtotal) {
        CouponCode code;
        try {
            code = CouponCode.of(rawCode);
        } catch (IllegalArgumentException e) {
            return new CouponPricing.NotFound(rawCode == null ? "" : rawCode);
        }
        CouponEvaluation result = couponQuery.evaluate(code, subtotal);
        return switch (result) {
            case CouponEvaluation.Applicable a    -> new CouponPricing.Applicable(a.code().value(), a.discount(), a.subtotal());
            case CouponEvaluation.NotFound nf     -> new CouponPricing.NotFound(nf.code().value());
            case CouponEvaluation.Inactive i      -> new CouponPricing.Inactive(i.code().value());
            case CouponEvaluation.Expired e       -> new CouponPricing.Expired(e.code().value());
            case CouponEvaluation.Exhausted ex    -> new CouponPricing.Exhausted(ex.code().value());
            case CouponEvaluation.BelowMinOrder b -> new CouponPricing.BelowMinOrder(b.code().value(), b.minOrder(), b.subtotal());
        };
    }
}
