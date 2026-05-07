package com.cardapio.promotion.domain.model;

import com.cardapio.promotion.domain.exception.CouponInvariantException;
import com.cardapio.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    private final Currency BRL = Currency.getInstance("BRL");
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T12:00:00Z"), ZoneOffset.UTC);

    private Coupon percentCoupon(String value) {
        return Coupon.create(CouponCode.of("WELCOME"), DiscountType.PERCENT,
            new BigDecimal(value), BRL, null, null, null, null, clock);
    }

    private Coupon fixedCoupon(String value) {
        return Coupon.create(CouponCode.of("BLACK5"), DiscountType.FIXED,
            new BigDecimal(value), BRL, null, null, null, null, clock);
    }

    @Test
    void percentDiscountOnSubtotal() {
        Coupon c = percentCoupon("10");
        Money discount = c.discountFor(Money.brl("100.00"));
        assertThat(discount.amount()).isEqualByComparingTo("10.00");
    }

    @Test
    void fixedClampsToSubtotal() {
        Coupon c = fixedCoupon("25");
        Money discount = c.discountFor(Money.brl("10.00"));
        assertThat(discount.amount()).isEqualByComparingTo("10.00");
    }

    @Test
    void percentAbove100Rejected() {
        assertThatThrownBy(() -> Coupon.create(CouponCode.of("BAD"), DiscountType.PERCENT,
            new BigDecimal("150"), BRL, null, null, null, null, clock))
            .isInstanceOf(CouponInvariantException.class);
    }

    @Test
    void valueMustBePositive() {
        assertThatThrownBy(() -> Coupon.create(CouponCode.of("ZERO"), DiscountType.PERCENT,
            BigDecimal.ZERO, BRL, null, null, null, null, clock))
            .isInstanceOf(CouponInvariantException.class);
    }

    @Test
    void minOrderCurrencyMustMatch() {
        Currency USD = Currency.getInstance("USD");
        Money usdMin = Money.of(new BigDecimal("50"), USD);
        assertThatThrownBy(() -> Coupon.create(CouponCode.of("USD"), DiscountType.PERCENT,
            new BigDecimal("10"), BRL, null, null, usdMin, null, clock))
            .isInstanceOf(CouponInvariantException.class);
    }

    @Test
    void incrementUsesBlocksAtMax() {
        Coupon c = Coupon.create(CouponCode.of("ONESHOT"), DiscountType.FIXED,
            new BigDecimal("5"), BRL, null, null, null, 1, clock);
        c.incrementUses(clock);
        assertThat(c.isExhausted()).isTrue();
        assertThatThrownBy(() -> c.incrementUses(clock))
            .isInstanceOf(CouponInvariantException.class);
    }

    @Test
    void decrementClampsAtZero() {
        Coupon c = percentCoupon("10");
        c.decrementUses(clock);
        assertThat(c.usesCount()).isZero();
    }

    @Test
    void meetsMinOrder() {
        Coupon c = Coupon.create(CouponCode.of("MIN50"), DiscountType.PERCENT,
            new BigDecimal("10"), BRL, null, null, Money.brl("50.00"), null, clock);
        assertThat(c.meetsMinOrder(Money.brl("49.99"))).isFalse();
        assertThat(c.meetsMinOrder(Money.brl("50.00"))).isTrue();
    }

    @Test
    void withinDateWindow() {
        Instant from = Instant.parse("2026-05-01T00:00:00Z");
        Instant until = Instant.parse("2026-06-01T00:00:00Z");
        Coupon c = Coupon.create(CouponCode.of("DATED"), DiscountType.PERCENT,
            new BigDecimal("10"), BRL, from, until, null, null, clock);
        assertThat(c.withinDateWindow(Instant.parse("2026-05-15T00:00:00Z"))).isTrue();
        assertThat(c.withinDateWindow(Instant.parse("2026-04-30T00:00:00Z"))).isFalse();
        assertThat(c.withinDateWindow(Instant.parse("2026-06-01T00:00:00Z"))).isFalse();
    }

    @Test
    void rejectsValidUntilBeforeValidFrom() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant until = Instant.parse("2026-05-01T00:00:00Z");
        assertThatThrownBy(() -> Coupon.create(CouponCode.of("BAD"), DiscountType.PERCENT,
            new BigDecimal("10"), BRL, from, until, null, null, clock))
            .isInstanceOf(CouponInvariantException.class);
    }

    @Test
    void deactivateAndReactivate() {
        Coupon c = percentCoupon("10");
        c.deactivate(clock);
        assertThat(c.isActive()).isFalse();
        c.activate(clock);
        assertThat(c.isActive()).isTrue();
    }

    @Test
    void updateRejectsLoweringMaxUsesBelowUsesCount() {
        Coupon c = Coupon.create(CouponCode.of("UPD"), DiscountType.FIXED,
            new BigDecimal("5"), BRL, null, null, null, 10, clock);
        c.incrementUses(clock);
        c.incrementUses(clock);
        assertThatThrownBy(() -> c.update(null, null, null, 1, clock))
            .isInstanceOf(CouponInvariantException.class);
    }
}
