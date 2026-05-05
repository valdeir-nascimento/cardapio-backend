package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.SetProductStockCommand;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.model.Stock;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SetProductStockUseCase {
    private final ProductRepository products;
    public SetProductStockUseCase(ProductRepository products) { this.products = products; }

    @Transactional
    public Result<Void> execute(SetProductStockCommand cmd) {
        Notification n = Notification.empty();
        if (cmd.quantity() != null && cmd.quantity() < 0) {
            n.addError("quantity", "INVALID_STOCK", "estoque inválido");
            return Result.failure(n);
        }
        Optional<Product> maybe = products.findById(cmd.id());
        if (maybe.isEmpty()) {
            n.addError("PRODUCT_NOT_FOUND", "produto não encontrado");
            return Result.failure(n);
        }
        Product p = maybe.get();
        p.changeStock(cmd.quantity() == null ? Stock.untracked() : Stock.of(cmd.quantity()));
        products.save(p);
        return Result.success(null);
    }
}
