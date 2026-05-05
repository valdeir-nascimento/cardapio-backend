package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCategoryUseCase {

    private final CategoryRepository categories;
    private final ProductRepository products;

    @Transactional
    public Result<Void> execute(CategoryId id) {
        if (!categories.existsById(id)) {
            return Result.failWith(ErrorCode.CATEGORY_NOT_FOUND);
        }
        if (products.countByCategory(id) > 0) {
            return Result.failWith(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }
        categories.deleteById(id);
        return Result.ok();
    }
}
