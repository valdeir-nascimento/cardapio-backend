package com.cardapio.ordering.infrastructure.persistence.repository;

import com.cardapio.ordering.infrastructure.persistence.jpa.CartJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringCartJpaRepository extends JpaRepository<CartJpaEntity, UUID> {
    Optional<CartJpaEntity> findByCustomerId(UUID customerId);
}
