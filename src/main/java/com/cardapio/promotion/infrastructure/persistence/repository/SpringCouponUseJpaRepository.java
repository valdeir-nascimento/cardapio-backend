package com.cardapio.promotion.infrastructure.persistence.repository;

import com.cardapio.promotion.infrastructure.persistence.jpa.CouponUseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface SpringCouponUseJpaRepository extends JpaRepository<CouponUseJpaEntity, CouponUseJpaEntity.PK> {

    boolean existsByCouponCodeAndOrderId(String couponCode, UUID orderId);

    @Modifying
    @Transactional
    @Query("delete from CouponUseJpaEntity u where u.couponCode = :code and u.orderId = :orderId")
    void deleteByCouponCodeAndOrderId(@Param("code") String code, @Param("orderId") UUID orderId);
}
