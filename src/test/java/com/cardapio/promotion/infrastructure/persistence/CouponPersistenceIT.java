package com.cardapio.promotion.infrastructure.persistence;

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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class CouponPersistenceIT {

    @Autowired CouponRepository coupons;
    @Autowired CouponUseRepository couponUses;

    private final Currency BRL = Currency.getInstance("BRL");
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void couponRoundTrip() {
        CouponCode code = CouponCode.of("ROUND-" + uniqueSuffix());
        Coupon c = Coupon.create(code, DiscountType.PERCENT, new BigDecimal("15"), BRL,
            null, Instant.parse("2026-12-01T00:00:00Z"), Money.brl("30.00"), 5, clock);
        coupons.save(c);

        var loaded = coupons.findByCode(code).orElseThrow();
        assertThat(loaded.code()).isEqualTo(code);
        assertThat(loaded.value()).isEqualByComparingTo("15.00");
        assertThat(loaded.minOrderValue().orElseThrow().amount()).isEqualByComparingTo("30.00");
        assertThat(loaded.maxUses()).contains(5);
        assertThat(loaded.usesCount()).isZero();
        assertThat(loaded.isActive()).isTrue();
    }

    @Test
    void incrementUsesPersists() {
        CouponCode code = CouponCode.of("INC-" + uniqueSuffix());
        Coupon c = Coupon.create(code, DiscountType.FIXED, new BigDecimal("5"), BRL,
            null, null, null, null, clock);
        coupons.save(c);

        Coupon loaded = coupons.findByCode(code).orElseThrow();
        loaded.incrementUses(clock);
        coupons.save(loaded);

        Coupon reloaded = coupons.findByCode(code).orElseThrow();
        assertThat(reloaded.usesCount()).isEqualTo(1);
    }

    @Test
    void couponUseLedgerIsIdempotentByPrimaryKey() {
        CouponCode code = CouponCode.of("LEDGER-" + uniqueSuffix());
        UUID orderId = UUID.randomUUID();
        Instant now = clock.instant();

        assertThat(couponUses.exists(code, orderId)).isFalse();
        couponUses.register(code, orderId, now);
        assertThat(couponUses.exists(code, orderId)).isTrue();

        couponUses.remove(code, orderId);
        assertThat(couponUses.exists(code, orderId)).isFalse();
    }

    @Test
    void findAllByActiveAndPrefix() {
        coupons.save(Coupon.create(CouponCode.of("PROMO-A-" + uniqueSuffix()),
            DiscountType.PERCENT, new BigDecimal("5"), BRL, null, null, null, null, clock));
        Coupon inactive = Coupon.create(CouponCode.of("PROMO-B-" + uniqueSuffix()),
            DiscountType.PERCENT, new BigDecimal("5"), BRL, null, null, null, null, clock);
        inactive.deactivate(clock);
        coupons.save(inactive);

        var all = coupons.findAll(false, "PROMO-", 50, 0);
        var activeOnly = coupons.findAll(true, "PROMO-", 50, 0);

        assertThat(all.size()).isGreaterThanOrEqualTo(activeOnly.size());
        assertThat(activeOnly).allMatch(Coupon::isActive);
    }

    private String uniqueSuffix() {
        return Integer.toHexString(Math.abs((int) System.nanoTime())).toUpperCase();
    }
}
