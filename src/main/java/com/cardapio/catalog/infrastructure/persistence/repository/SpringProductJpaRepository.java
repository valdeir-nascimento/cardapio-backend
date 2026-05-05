package com.cardapio.catalog.infrastructure.persistence.repository;

import com.cardapio.catalog.infrastructure.persistence.jpa.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {
    List<ProductJpaEntity> findAllByCategoryIdAndAvailableTrueOrderByName(UUID categoryId);
    long countByCategoryId(UUID categoryId);
}
