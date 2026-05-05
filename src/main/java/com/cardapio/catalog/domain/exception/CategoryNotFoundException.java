package com.cardapio.catalog.domain.exception;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.shared.domain.DomainException;

public class CategoryNotFoundException extends DomainException {
    public CategoryNotFoundException(CategoryId id) {
        super("CATEGORY_NOT_FOUND", "category not found: " + id.value());
    }
}
