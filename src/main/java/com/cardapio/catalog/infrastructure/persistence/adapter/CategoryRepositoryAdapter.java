package com.cardapio.catalog.infrastructure.persistence.adapter;

import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.infrastructure.persistence.mapper.CategoryMapper;
import com.cardapio.catalog.infrastructure.persistence.repository.SpringCategoryJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Component
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final SpringCategoryJpaRepository jpa;
    private final Clock clock;

    public CategoryRepositoryAdapter(SpringCategoryJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public void save(Category category) {
        var existing = jpa.findById(category.id().value());
        if (existing.isPresent()) {
            CategoryMapper.updateJpa(existing.get(), category);
            jpa.save(existing.get());
        } else {
            jpa.save(CategoryMapper.toJpa(category, clock.instant()));
        }
    }

    @Override public Optional<Category> findById(CategoryId id) {
        return jpa.findById(id.value()).map(CategoryMapper::toDomain);
    }

    @Override public List<Category> findAll() {
        return jpa.findAllByOrderByDisplayOrderAsc().stream().map(CategoryMapper::toDomain).toList();
    }

    @Override public List<Category> findAllActive() {
        return jpa.findAllByActiveTrueOrderByDisplayOrderAsc().stream().map(CategoryMapper::toDomain).toList();
    }

    @Override public void deleteById(CategoryId id) { jpa.deleteById(id.value()); }
    @Override public boolean existsById(CategoryId id) { return jpa.existsById(id.value()); }
}
