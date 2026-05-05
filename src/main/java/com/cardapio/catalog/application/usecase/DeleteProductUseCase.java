package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteProductUseCase extends WriteProductUseCase {

    public DeleteProductUseCase(ProductRepository products) {
        super(products);
    }

    @Transactional
    public Result<Void> execute(ProductId id) {
        return loadProduct(id).flatMap(p -> {
            products.deleteById(id);
            return Result.ok();
        });
    }
}
