package com.cardapio.promotion.domain.model;

import com.cardapio.promotion.domain.exception.CouponInvariantException;
import com.cardapio.shared.domain.AggregateRoot;
import com.cardapio.shared.domain.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.Optional;

public final class Coupon extends AggregateRoot<CouponId> {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CouponCode code;
    private final DiscountType type;
    private BigDecimal value;
    private final Currency currency;
    private final Instant validFrom;
    private Instant validUntil;
    private Money minOrderValue;
    private Integer maxUses;
    private int usesCount;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Coupon(CouponId id, CouponCode code, DiscountType type, BigDecimal value, Currency currency,
                   Instant validFrom, Instant validUntil, Money minOrderValue, Integer maxUses,
                   int usesCount, boolean active, Instant createdAt, Instant updatedAt) {
        super(id);
        this.code = Objects.requireNonNull(code, "code");
        this.type = Objects.requireNonNull(type, "type");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.value = normalize(type, currency, value);
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.minOrderValue = minOrderValue;
        this.maxUses = maxUses;
        this.usesCount = usesCount;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        enforceInvariants();
    }

    private void enforceInvariants() {
        if (validFrom != null && validUntil != null && !validUntil.isAfter(validFrom)) {
            throw new CouponInvariantException("validUntil must be after validFrom");
        }
        if (minOrderValue != null && !minOrderValue.currency().equals(currency)) {
            throw new CouponInvariantException("minOrderValue currency must match coupon currency");
        }
        if (maxUses != null && maxUses < 0) {
            throw new CouponInvariantException("maxUses must be non-negative");
        }
        if (usesCount < 0) {
            throw new CouponInvariantException("usesCount must be non-negative");
        }
    }

    private static BigDecimal normalize(DiscountType type, Currency currency, BigDecimal value) {
        Objects.requireNonNull(value, "value");
        if (value.signum() <= 0) {
            throw new CouponInvariantException("value must be positive");
        }
        if (type == DiscountType.PERCENT) {
            if (value.compareTo(HUNDRED) > 0) {
                throw new CouponInvariantException("PERCENT value must be <= 100");
            }
            return value.setScale(2, RoundingMode.HALF_EVEN);
        }
        return value.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
    }

    public static Coupon create(CouponCode code, DiscountType type, BigDecimal value, Currency currency,
                                Instant validFrom, Instant validUntil,
                                Money minOrderValue, Integer maxUses, Clock clock) {
        Objects.requireNonNull(clock, "clock");
        Instant now = clock.instant();
        return new Coupon(CouponId.newId(), code, type, value, currency,
            validFrom, validUntil, minOrderValue, maxUses, 0, true, now, now);
    }

    public static Coupon rehydrate(CouponId id, CouponCode code, DiscountType type, BigDecimal value, Currency currency,
                                   Instant validFrom, Instant validUntil, Money minOrderValue, Integer maxUses,
                                   int usesCount, boolean active, Instant createdAt, Instant updatedAt) {
        return new Coupon(id, code, type, value, currency, validFrom, validUntil,
            minOrderValue, maxUses, usesCount, active, createdAt, updatedAt);
    }

    public boolean isActiveAt(Instant now) {
        return active && withinDateWindow(now);
    }

    public boolean withinDateWindow(Instant now) {
        if (validFrom != null && now.isBefore(validFrom)) return false;
        if (validUntil != null && !now.isBefore(validUntil)) return false;
        return true;
    }

    public boolean isExhausted() {
        return maxUses != null && usesCount >= maxUses;
    }

    public boolean meetsMinOrder(Money subtotal) {
        if (minOrderValue == null) return true;
        if (!subtotal.currency().equals(currency)) {
            throw new CouponInvariantException("subtotal currency mismatch");
        }
        return subtotal.amount().compareTo(minOrderValue.amount()) >= 0;
    }

    public Money discountFor(Money subtotal) {
        if (!subtotal.currency().equals(currency)) {
            throw new CouponInvariantException("subtotal currency mismatch");
        }
        BigDecimal raw = switch (type) {
            case PERCENT -> subtotal.amount()
                .multiply(value)
                .divide(HUNDRED, currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
            case FIXED -> value.min(subtotal.amount());
        };
        return Money.of(raw, currency);
    }

    public void update(BigDecimal newValue, Instant newValidUntil, Money newMinOrderValue,
                       Integer newMaxUses, Clock clock) {
        if (newValue != null) {
            this.value = normalize(type, currency, newValue);
        }
        if (newValidUntil != null) {
            this.validUntil = newValidUntil;
        }
        if (newMinOrderValue != null) {
            if (!newMinOrderValue.currency().equals(currency)) {
                throw new CouponInvariantException("minOrderValue currency must match coupon currency");
            }
            this.minOrderValue = newMinOrderValue;
        }
        if (newMaxUses != null) {
            if (newMaxUses < usesCount) {
                throw new CouponInvariantException("maxUses cannot be lower than usesCount");
            }
            this.maxUses = newMaxUses;
        }
        this.updatedAt = clock.instant();
        enforceInvariants();
    }

    public void activate(Clock clock) {
        this.active = true;
        this.updatedAt = clock.instant();
    }

    public void deactivate(Clock clock) {
        this.active = false;
        this.updatedAt = clock.instant();
    }

    public void incrementUses(Clock clock) {
        if (isExhausted()) {
            throw new CouponInvariantException("coupon exhausted");
        }
        this.usesCount += 1;
        this.updatedAt = clock.instant();
    }

    public void decrementUses(Clock clock) {
        if (usesCount > 0) {
            this.usesCount -= 1;
            this.updatedAt = clock.instant();
        }
    }

    public CouponCode code() { return code; }
    public DiscountType type() { return type; }
    public BigDecimal value() { return value; }
    public Currency currency() { return currency; }
    public Optional<Instant> validFrom() { return Optional.ofNullable(validFrom); }
    public Optional<Instant> validUntil() { return Optional.ofNullable(validUntil); }
    public Optional<Money> minOrderValue() { return Optional.ofNullable(minOrderValue); }
    public Optional<Integer> maxUses() { return Optional.ofNullable(maxUses); }
    public int usesCount() { return usesCount; }
    public boolean isActive() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
