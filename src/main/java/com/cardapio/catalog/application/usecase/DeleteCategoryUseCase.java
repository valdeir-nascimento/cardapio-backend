// DeleteCategoryUseCase.java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteCategoryUseCase {
    private final CategoryRepository categories;
    private final ProductRepository products;

    public DeleteCategoryUseCase(CategoryRepository categories, ProductRepository products) {
        this.categories = categories; this.products = products;
    }

    @Transactional
    public Result<Void> execute(CategoryId id) {
        Notification n = Notification.empty();
        if (!categories.existsById(id)) {
            n.addError("CATEGORY_NOT_FOUND", "categoria não encontrada");
            return Result.failure(n);
        }
        long pCount = products.countByCategory(id);
        if (pCount > 0) {
            n.addError("CATEGORY_HAS_PRODUCTS", "categoria tem produtos vinculados");
            return Result.failure(n);
        }
        categories.deleteById(id);
        return Result.success(null);
    }
}
