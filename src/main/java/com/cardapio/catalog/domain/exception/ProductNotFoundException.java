package com.cardapio.catalog.domain.exception;

import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.shared.domain.DomainException;

public class ProductNotFoundException extends DomainException {
    public ProductNotFoundException(ProductId id) {
        super("PRODUCT_NOT_FOUND", "product not found: " + id.value());
    }
}
