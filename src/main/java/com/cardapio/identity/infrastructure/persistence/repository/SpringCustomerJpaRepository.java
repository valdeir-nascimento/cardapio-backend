package com.cardapio.identity.infrastructure.persistence.repository;

import com.cardapio.identity.infrastructure.persistence.jpa.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringCustomerJpaRepository extends JpaRepository<CustomerJpaEntity, UUID> {
    Optional<CustomerJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
