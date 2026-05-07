package com.cardapio.promotion.domain.dto;

import com.cardapio.promotion.domain.model.CouponCode;
import com.cardapio.shared.domain.Money;

public sealed interface CouponEvaluation {

    CouponCode code();

    record Applicable(CouponCode code, Money discount, Money subtotal) implements CouponEvaluation {}
    record NotFound(CouponCode code) implements CouponEvaluation {}
    record Inactive(CouponCode code) implements CouponEvaluation {}
    record Expired(CouponCode code) implements CouponEvaluation {}
    record BelowMinOrder(CouponCode code, Money minOrder, Money subtotal) implements CouponEvaluation {}
    record Exhausted(CouponCode code) implements CouponEvaluation {}
}
