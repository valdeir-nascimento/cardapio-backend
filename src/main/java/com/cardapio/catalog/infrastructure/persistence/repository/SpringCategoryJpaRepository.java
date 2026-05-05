package com.cardapio.catalog.infrastructure.persistence.repository;

import com.cardapio.catalog.infrastructure.persistence.jpa.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringCategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {
    List<CategoryJpaEntity> findAllByOrderByDisplayOrderAsc();
    List<CategoryJpaEntity> findAllByActiveTrueOrderByDisplayOrderAsc();
}
