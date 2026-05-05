package com.cardapio.catalog.infrastructure.persistence.mapper;

import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.infrastructure.persistence.jpa.CategoryJpaEntity;

import java.time.Instant;

public final class CategoryMapper {
    private CategoryMapper() {}

    public static CategoryJpaEntity toJpa(Category c, Instant now) {
        return new CategoryJpaEntity(c.id().value(), c.name(), c.displayOrder(), c.isActive(), now);
    }

    public static void updateJpa(CategoryJpaEntity entity, Category c) {
        entity.setName(c.name());
        entity.setDisplayOrder(c.displayOrder());
        entity.setActive(c.isActive());
    }

    public static Category toDomain(CategoryJpaEntity e) {
        return Category.rehydrate(CategoryId.of(e.getId()), e.getName(), e.getDisplayOrder(), e.isActive());
    }
}
