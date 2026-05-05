package com.cardapio.catalog.domain.port;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.model.ProductId;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    void save(Product product);
    Optional<Product> findById(ProductId id);
    List<Product> findAll();
    List<Product> findAvailableByCategory(CategoryId categoryId);
    void deleteById(ProductId id);
    long countByCategory(CategoryId categoryId);
}
