package com.cardapio.ordering.infrastructure.persistence.repository;

import com.cardapio.ordering.infrastructure.persistence.jpa.OrderJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface SpringOrderJpaRepository
    extends JpaRepository<OrderJpaEntity, UUID>, JpaSpecificationExecutor<OrderJpaEntity> {

    List<OrderJpaEntity> findAllByCustomerIdOrderByPlacedAtDesc(UUID customerId, Pageable pageable);
}
