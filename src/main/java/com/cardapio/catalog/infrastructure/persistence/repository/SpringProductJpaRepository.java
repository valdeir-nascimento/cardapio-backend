package com.cardapio.catalog.infrastructure.persistence.repository;

import com.cardapio.catalog.infrastructure.persistence.jpa.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    @Query("SELECT DISTINCT p FROM ProductJpaEntity p " +
        "LEFT JOIN FETCH p.variations " +
        "LEFT JOIN FETCH p.addOnGroups ag " +
        "LEFT JOIN FETCH ag.items " +
        "WHERE p.id = :id")
    Optional<ProductJpaEntity> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT DISTINCT p FROM ProductJpaEntity p " +
        "LEFT JOIN FETCH p.variations " +
        "LEFT JOIN FETCH p.addOnGroups ag " +
        "LEFT JOIN FETCH ag.items " +
        "WHERE p.categoryId = :categoryId AND p.available = true " +
        "ORDER BY p.name")
    List<ProductJpaEntity> findAvailableByCategoryWithDetails(@Param("categoryId") UUID categoryId);

    @Query("SELECT p FROM ProductJpaEntity p " +
        "WHERE p.categoryId IN :categoryIds AND p.available = true " +
        "ORDER BY p.categoryId, p.name")
    List<ProductJpaEntity> findAvailableByCategoryIds(@Param("categoryIds") List<UUID> categoryIds);

    long countByCategoryId(UUID categoryId);
}
