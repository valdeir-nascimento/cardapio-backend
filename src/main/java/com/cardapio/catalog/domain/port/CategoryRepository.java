package com.cardapio.catalog.domain.port;

import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    void save(Category category);
    Optional<Category> findById(CategoryId id);
    List<Category> findAll();
    List<Category> findAllActive();
    void deleteById(CategoryId id);
    boolean existsById(CategoryId id);
}
