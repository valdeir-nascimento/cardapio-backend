package com.cardapio.promotion.infrastructure.persistence.repository;

import com.cardapio.promotion.infrastructure.persistence.jpa.CouponJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SpringCouponJpaRepository
    extends JpaRepository<CouponJpaEntity, UUID>, JpaSpecificationExecutor<CouponJpaEntity> {

    Optional<CouponJpaEntity> findByCode(String code);
}
