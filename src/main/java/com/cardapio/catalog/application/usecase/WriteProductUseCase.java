package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;

public abstract class WriteProductUseCase {

    protected final ProductRepository products;

    protected WriteProductUseCase(ProductRepository products) {
        this.products = products;
    }

    protected Result<Product> loadProduct(ProductId id) {
        return Result.ofOptional(products.findById(id), ErrorCode.PRODUCT_NOT_FOUND);
    }
}
