package com.cardapio.promotion.application.usecase;

import com.cardapio.promotion.domain.dto.CouponEvaluation;
import com.cardapio.promotion.domain.model.Coupon;
import com.cardapio.promotion.domain.model.CouponCode;
import com.cardapio.promotion.domain.model.CouponId;
import com.cardapio.promotion.domain.model.DiscountType;
import com.cardapio.promotion.domain.port.CouponRepository;
import com.cardapio.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluateCouponUseCaseTest {

    private final Currency BRL = Currency.getInstance("BRL");
    private final Instant fixed = Instant.parse("2026-05-15T12:00:00Z");
    private final Clock clock = Clock.fixed(fixed, ZoneOffset.UTC);
    private final CouponRepository repo = new InMemoryRepo();
    private final EvaluateCouponUseCase useCase = new EvaluateCouponUseCase(repo, clock);

    private Coupon save(Coupon c) { repo.save(c); return c; }

    @Test
    void notFound() {
        var result = useCase.evaluate(CouponCode.of("MISSING"), Money.brl("100.00"));
        assertThat(result).isInstanceOf(CouponEvaluation.NotFound.class);
    }

    @Test
    void inactive() {
        var c = save(Coupon.create(CouponCode.of("OFF"), DiscountType.PERCENT, new BigDecimal("10"),
            BRL, null, null, null, null, clock));
        c.deactivate(clock);
        repo.save(c);
        var result = useCase.evaluate(c.code(), Money.brl("100.00"));
        assertThat(result).isInstanceOf(CouponEvaluation.Inactive.class);
    }

    @Test
    void expired() {
        save(Coupon.create(CouponCode.of("OLD"), DiscountType.PERCENT, new BigDecimal("10"),
            BRL, null, Instant.parse("2026-01-01T00:00:00Z"), null, null, clock));
        var result = useCase.evaluate(CouponCode.of("OLD"), Money.brl("100.00"));
        assertThat(result).isInstanceOf(CouponEvaluation.Expired.class);
    }

    @Test
    void exhausted() {
        var c = save(Coupon.create(CouponCode.of("ONE"), DiscountType.FIXED, new BigDecimal("5"),
            BRL, null, null, null, 1, clock));
        c.incrementUses(clock);
        repo.save(c);
        var result = useCase.evaluate(c.code(), Money.brl("100.00"));
        assertThat(result).isInstanceOf(CouponEvaluation.Exhausted.class);
    }

    @Test
    void belowMinOrder() {
        save(Coupon.create(CouponCode.of("MIN50"), DiscountType.PERCENT, new BigDecimal("10"),
            BRL, null, null, Money.brl("50.00"), null, clock));
        var result = useCase.evaluate(CouponCode.of("MIN50"), Money.brl("49.99"));
        assertThat(result).isInstanceOf(CouponEvaluation.BelowMinOrder.class);
    }

    @Test
    void applicableComputesDiscount() {
        save(Coupon.create(CouponCode.of("WELCOME10"), DiscountType.PERCENT, new BigDecimal("10"),
            BRL, null, null, null, null, clock));
        var result = useCase.evaluate(CouponCode.of("WELCOME10"), Money.brl("100.00"));
        assertThat(result).isInstanceOfSatisfying(CouponEvaluation.Applicable.class, applicable ->
            assertThat(applicable.discount().amount()).isEqualByComparingTo("10.00"));
    }

    private static class InMemoryRepo implements CouponRepository {
        private final Map<String, Coupon> byCode = new HashMap<>();
        @Override public void save(Coupon c) { byCode.put(c.code().value(), c); }
        @Override public Optional<Coupon> findById(CouponId id) {
            return byCode.values().stream().filter(c -> c.id().equals(id)).findFirst();
        }
        @Override public Optional<Coupon> findByCode(CouponCode code) {
            return Optional.ofNullable(byCode.get(code.value()));
        }
        @Override public List<Coupon> findAll(boolean activeOnly, String prefix, int limit, int offset) {
            return List.copyOf(byCode.values());
        }
        @Override public long count(boolean activeOnly, String prefix) { return byCode.size(); }
    }
}
