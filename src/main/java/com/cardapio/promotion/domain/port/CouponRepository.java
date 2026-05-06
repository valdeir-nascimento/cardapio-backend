package com.cardapio.promotion.domain.port;

import com.cardapio.promotion.domain.model.Coupon;
import com.cardapio.promotion.domain.model.CouponCode;
import com.cardapio.promotion.domain.model.CouponId;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    void save(Coupon coupon);
    Optional<Coupon> findById(CouponId id);
    Optional<Coupon> findByCode(CouponCode code);
    List<Coupon> findAll(boolean activeOnly, String codePrefix, int limit, int offset);
    long count(boolean activeOnly, String codePrefix);
}
