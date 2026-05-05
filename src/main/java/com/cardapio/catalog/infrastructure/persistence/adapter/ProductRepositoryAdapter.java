package com.cardapio.catalog.infrastructure.persistence.adapter;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.catalog.infrastructure.persistence.mapper.ProductMapper;
import com.cardapio.catalog.infrastructure.persistence.repository.SpringProductJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final SpringProductJpaRepository jpa;
    private final Clock clock;

    @Override
    public void save(Product product) {
        var existing = jpa.findByIdWithDetails(product.id().value());
        if (existing.isPresent()) {
            ProductMapper.updateJpa(existing.get(), product, clock.instant());
            jpa.save(existing.get());
        } else {
            jpa.save(ProductMapper.toJpa(product, clock.instant()));
        }
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return jpa.findByIdWithDetails(id.value()).map(ProductMapper::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return jpa.findAll().stream().map(ProductMapper::toDomain).toList();
    }

    @Override
    public List<Product> findAvailableByCategory(CategoryId categoryId) {
        return jpa.findAvailableByCategoryWithDetails(categoryId.value())
            .stream().map(ProductMapper::toDomain).toList();
    }

    @Override
    public Map<CategoryId, List<Product>> findAvailableGroupedByCategories(List<CategoryId> categoryIds) {
        if (categoryIds.isEmpty()) return Map.of();
        List<UUID> rawIds = categoryIds.stream().map(CategoryId::value).toList();
        Map<CategoryId, List<Product>> result = new HashMap<>();
        for (CategoryId id : categoryIds) result.put(id, List.of());
        Map<UUID, List<Product>> grouped = jpa.findAvailableByCategoryIds(rawIds).stream()
            .map(ProductMapper::toDomain)
            .collect(Collectors.groupingBy(p -> p.categoryId().value()));
        grouped.forEach((rawId, ps) -> result.put(CategoryId.of(rawId), ps));
        return result;
    }

    @Override
    public void deleteById(ProductId id) {
        jpa.deleteById(id.value());
    }

    @Override
    public long countByCategory(CategoryId categoryId) {
        return jpa.countByCategoryId(categoryId.value());
    }
}
