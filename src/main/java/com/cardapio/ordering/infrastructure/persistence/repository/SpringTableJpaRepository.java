package com.cardapio.ordering.infrastructure.persistence.repository;

import com.cardapio.ordering.infrastructure.persistence.jpa.TableJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringTableJpaRepository extends JpaRepository<TableJpaEntity, UUID> {
    Optional<TableJpaEntity> findByQrToken(UUID qrToken);
    Optional<TableJpaEntity> findByNumber(Integer number);
    List<TableJpaEntity> findAllByOrderByNumberAsc();
}
