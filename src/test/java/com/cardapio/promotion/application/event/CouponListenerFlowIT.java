package com.cardapio.promotion.application.event;

import com.cardapio.ordering.domain.event.OrderCanceled;
import com.cardapio.ordering.domain.event.OrderPlaced;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.promotion.domain.model.Coupon;
import com.cardapio.promotion.domain.model.CouponCode;
import com.cardapio.promotion.domain.model.DiscountType;
import com.cardapio.promotion.domain.port.CouponRepository;
import com.cardapio.promotion.domain.port.CouponUseRepository;
import com.cardapio.shared.domain.Money;
import com.cardapio.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class CouponListenerFlowIT {

    @Autowired CouponRepository coupons;
    @Autowired CouponUseRepository couponUses;
    @Autowired ApplicationEventPublisher events;
    @Autowired TransactionTemplate tx;

    private void publishInTx(Object event) {
        tx.executeWithoutResult(s -> events.publishEvent(event));
    }

    private final Currency BRL = Currency.getInstance("BRL");
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void orderPlacedIncrementsUsesAndIsIdempotent() {
        CouponCode code = CouponCode.of("FLOW-" + suffix());
        coupons.save(Coupon.create(code, DiscountType.PERCENT, new BigDecimal("10"), BRL,
            null, null, null, null, clock));

        OrderId orderId = OrderId.newId();
        publishInTx(OrderPlaced.of(orderId, UUID.randomUUID(), OrderModality.PICKUP,
            Money.brl("90.00"), code.value(), Instant.now()));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
            assertThat(couponUses.exists(code, orderId.value())).isTrue());
        Coupon afterFirst = coupons.findByCode(code).orElseThrow();
        assertThat(afterFirst.usesCount()).isEqualTo(1);

        // Duplicate event delivery — must not double-count
        publishInTx(OrderPlaced.of(orderId, UUID.randomUUID(), OrderModality.PICKUP,
            Money.brl("90.00"), code.value(), Instant.now()));

        // give the listener a beat to (no-op) handle the dup
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        Coupon afterDup = coupons.findByCode(code).orElseThrow();
        assertThat(afterDup.usesCount()).isEqualTo(1);
    }

    @Test
    void orderCanceledDecrementsUsesAndPurgesLedger() {
        CouponCode code = CouponCode.of("CANCEL-" + suffix());
        coupons.save(Coupon.create(code, DiscountType.FIXED, new BigDecimal("5"), BRL,
            null, null, null, null, clock));

        OrderId orderId = OrderId.newId();
        UUID customerId = UUID.randomUUID();

        publishInTx(OrderPlaced.of(orderId, customerId, OrderModality.PICKUP,
            Money.brl("100.00"), code.value(), Instant.now()));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
            assertThat(coupons.findByCode(code).orElseThrow().usesCount()).isEqualTo(1));

        publishInTx(OrderCanceled.of(orderId, customerId, code.value(), Instant.now()));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(coupons.findByCode(code).orElseThrow().usesCount()).isZero();
            assertThat(couponUses.exists(code, orderId.value())).isFalse();
        });
    }

    @Test
    void cancelWithoutPriorPlaceIsNoOp() {
        CouponCode code = CouponCode.of("ORPHAN-" + suffix());
        coupons.save(Coupon.create(code, DiscountType.PERCENT, new BigDecimal("10"), BRL,
            null, null, null, null, clock));

        publishInTx(OrderCanceled.of(OrderId.newId(), UUID.randomUUID(),
            code.value(), Instant.now()));

        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        Coupon coupon = coupons.findByCode(code).orElseThrow();
        assertThat(coupon.usesCount()).isZero();
    }

    private String suffix() {
        return Integer.toHexString(Math.abs((int) System.nanoTime())).toUpperCase();
    }
}
