package com.cardapio.ordering.infrastructure.persistence.repository;

import com.cardapio.ordering.infrastructure.persistence.jpa.IdempotencyKeyJpaEntity;
import com.cardapio.ordering.infrastructure.persistence.jpa.IdempotencyKeyJpaEntity.IdempotencyKeyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringIdempotencyJpaRepository extends JpaRepository<IdempotencyKeyJpaEntity, IdempotencyKeyId> {
}
