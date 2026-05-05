package com.cardapio.identity.infrastructure.persistence.repository;

import com.cardapio.identity.infrastructure.persistence.jpa.AdminJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringAdminJpaRepository extends JpaRepository<AdminJpaEntity, UUID> {
    Optional<AdminJpaEntity> findByEmail(String email);
}
