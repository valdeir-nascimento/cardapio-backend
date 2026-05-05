package com.cardapio.catalog.infrastructure.persistence.adapter;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.catalog.infrastructure.persistence.mapper.ProductMapper;
import com.cardapio.catalog.infrastructure.persistence.repository.SpringProductJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Component
public class ProductRepositoryAdapter implements ProductRepository {

    private final SpringProductJpaRepository jpa;
    private final Clock clock;

    public ProductRepositoryAdapter(SpringProductJpaRepository jpa, Clock clock) {
        this.jpa = jpa; this.clock = clock;
    }

    @Override
    public void save(Product product) {
        var existing = jpa.findById(product.id().value());
        if (existing.isPresent()) {
            ProductMapper.updateJpa(existing.get(), product, clock.instant());
            jpa.save(existing.get());
        } else {
            jpa.save(ProductMapper.toJpa(product, clock.instant()));
        }
    }

    @Override public Optional<Product> findById(ProductId id) {
        return jpa.findById(id.value()).map(ProductMapper::toDomain);
    }

    @Override public List<Product> findAll() {
        return jpa.findAll().stream().map(ProductMapper::toDomain).toList();
    }

    @Override public List<Product> findAvailableByCategory(CategoryId categoryId) {
        return jpa.findAllByCategoryIdAndAvailableTrueOrderByName(categoryId.value())
            .stream().map(ProductMapper::toDomain).toList();
    }

    @Override public void deleteById(ProductId id) { jpa.deleteById(id.value()); }
    @Override public long countByCategory(CategoryId categoryId) { return jpa.countByCategoryId(categoryId.value()); }
}
