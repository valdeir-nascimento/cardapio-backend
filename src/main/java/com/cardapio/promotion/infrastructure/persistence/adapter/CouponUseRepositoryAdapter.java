package com.cardapio.promotion.infrastructure.persistence.adapter;

import com.cardapio.promotion.domain.model.CouponCode;
import com.cardapio.promotion.domain.port.CouponUseRepository;
import com.cardapio.promotion.infrastructure.persistence.jpa.CouponUseJpaEntity;
import com.cardapio.promotion.infrastructure.persistence.repository.SpringCouponUseJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CouponUseRepositoryAdapter implements CouponUseRepository {

    private final SpringCouponUseJpaRepository jpa;

    @Override
    public boolean exists(CouponCode code, UUID orderId) {
        return jpa.existsByCouponCodeAndOrderId(code.value(), orderId);
    }

    @Override
    public void register(CouponCode code, UUID orderId, Instant appliedAt) {
        jpa.save(new CouponUseJpaEntity(code.value(), orderId, appliedAt));
    }

    @Override
    public void remove(CouponCode code, UUID orderId) {
        jpa.deleteByCouponCodeAndOrderId(code.value(), orderId);
    }
}
