package com.cardapio.promotion.infrastructure.persistence.mapper;

import com.cardapio.promotion.domain.model.Coupon;
import com.cardapio.promotion.domain.model.CouponCode;
import com.cardapio.promotion.domain.model.CouponId;
import com.cardapio.promotion.domain.model.DiscountType;
import com.cardapio.promotion.infrastructure.persistence.jpa.CouponJpaEntity;
import com.cardapio.shared.domain.Money;

import java.util.Currency;

public final class CouponMapper {

    private CouponMapper() {}

    public static CouponJpaEntity toJpa(Coupon coupon) {
        return new CouponJpaEntity(
            coupon.id().value(),
            coupon.code().value(),
            coupon.type().name(),
            coupon.value(),
            coupon.currency().getCurrencyCode(),
            coupon.validFrom().orElse(null),
            coupon.validUntil().orElse(null),
            coupon.minOrderValue().map(m -> m.amount()).orElse(null),
            coupon.maxUses().orElse(null),
            coupon.usesCount(),
            coupon.isActive(),
            coupon.createdAt(),
            coupon.updatedAt()
        );
    }

    public static void update(CouponJpaEntity entity, Coupon coupon) {
        entity.setValue(coupon.value());
        entity.setValidUntil(coupon.validUntil().orElse(null));
        entity.setMinOrderValue(coupon.minOrderValue().map(Money::amount).orElse(null));
        entity.setMaxUses(coupon.maxUses().orElse(null));
        entity.setUsesCount(coupon.usesCount());
        entity.setActive(coupon.isActive());
        entity.setUpdatedAt(coupon.updatedAt());
    }

    public static Coupon toDomain(CouponJpaEntity e) {
        Currency currency = Currency.getInstance(e.getCurrency());
        Money minOrderValue = e.getMinOrderValue() == null ? null
            : Money.of(e.getMinOrderValue(), currency);
        return Coupon.rehydrate(
            CouponId.of(e.getId()),
            CouponCode.of(e.getCode()),
            DiscountType.valueOf(e.getDiscountType()),
            e.getValue(),
            currency,
            e.getValidFrom(),
            e.getValidUntil(),
            minOrderValue,
            e.getMaxUses(),
            e.getUsesCount(),
            Boolean.TRUE.equals(e.getActive()),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
