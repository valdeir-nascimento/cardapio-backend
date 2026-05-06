package com.cardapio.ordering.infrastructure.persistence.repository;

import com.cardapio.ordering.infrastructure.persistence.jpa.ComandaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringComandaJpaRepository extends JpaRepository<ComandaJpaEntity, UUID> {
    Optional<ComandaJpaEntity> findFirstByTableIdAndStatus(UUID tableId, String status);
    List<ComandaJpaEntity> findAllByStatusOrderByOpenedAtDesc(String status);
}
