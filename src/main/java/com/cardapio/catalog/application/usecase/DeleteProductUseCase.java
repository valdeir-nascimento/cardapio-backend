package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteProductUseCase {
    private final ProductRepository products;
    public DeleteProductUseCase(ProductRepository products) { this.products = products; }

    @Transactional
    public Result<Void> execute(ProductId id) {
        if (products.findById(id).isEmpty()) {
            Notification n = Notification.empty();
            n.addError("PRODUCT_NOT_FOUND", "produto não encontrado");
            return Result.failure(n);
        }
        products.deleteById(id);
        return Result.success(null);
    }
}
