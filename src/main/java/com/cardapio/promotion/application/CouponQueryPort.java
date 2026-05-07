package com.cardapio.promotion.application;

import com.cardapio.promotion.domain.dto.CouponEvaluation;
import com.cardapio.promotion.domain.model.CouponCode;
import com.cardapio.shared.domain.Money;
import org.springframework.modulith.NamedInterface;

@NamedInterface("CouponQueryPort")
public interface CouponQueryPort {
    CouponEvaluation evaluate(CouponCode code, Money subtotal);
}
