package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.dto.CartView;
import com.cardapio.ordering.application.dto.CouponPricing;
import com.cardapio.ordering.domain.model.Cart;
import com.cardapio.ordering.domain.model.OrderItem;
import com.cardapio.ordering.domain.port.CartRepository;
import com.cardapio.ordering.domain.port.CouponPricingPort;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Money;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplyCouponUseCase {

    private final CartRepository carts;
    private final CartPricingService pricing;
    private final CouponPricingPort couponPricing;
    private final GetCartQuery getCart;
    private final Clock clock;

    @Transactional
    public Result<CartView> execute(UUID customerId, String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return Result.failWith(ErrorCode.INVALID_COUPON, "code must not be blank");
        }

        Cart cart = carts.findByCustomerId(customerId).orElse(null);
        if (cart == null || cart.isEmpty()) {
            return Result.failWith(ErrorCode.CART_EMPTY);
        }

        Result<List<OrderItem>> priced = pricing.price(cart);
        if (!priced.isSuccess()) {
            return Result.failure(((Result.Failure<List<OrderItem>>) priced).notification());
        }
        List<OrderItem> items = ((Result.Success<List<OrderItem>>) priced).value();
        Currency currency = items.get(0).lineTotal().currency();
        Money subtotal = items.stream().map(OrderItem::lineTotal)
            .reduce(Money.of(BigDecimal.ZERO, currency), Money::add);

        CouponPricing pricingResult = couponPricing.evaluate(rawCode, subtotal);
        Result<Void> mapped = mapPricing(pricingResult);
        if (!mapped.isSuccess()) {
            return Result.failure(((Result.Failure<Void>) mapped).notification());
        }

        CouponPricing.Applicable applicable = (CouponPricing.Applicable) pricingResult;
        cart.applyCoupon(applicable.code(), clock);
        carts.save(cart);
        return Result.success(getCart.getOrEmpty(customerId));
    }

    static Result<Void> mapPricing(CouponPricing pricing) {
        return switch (pricing) {
            case CouponPricing.Applicable a -> Result.ok();
            case CouponPricing.NotFound nf -> Result.failWith(ErrorCode.COUPON_NOT_FOUND);
            case CouponPricing.Inactive i -> Result.failWith(ErrorCode.COUPON_INACTIVE);
            case CouponPricing.Expired e -> Result.failWith(ErrorCode.COUPON_EXPIRED);
            case CouponPricing.Exhausted ex -> Result.failWith(ErrorCode.COUPON_EXHAUSTED);
            case CouponPricing.BelowMinOrder bm -> Result.failWith(ErrorCode.COUPON_BELOW_MIN_ORDER,
                "subtotal %s < minOrder %s".formatted(bm.subtotal().amount(), bm.minOrder().amount()));
        };
    }
}
