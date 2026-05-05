package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.SetProductAvailabilityCommand;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SetProductAvailabilityUseCase {
    private final ProductRepository products;
    public SetProductAvailabilityUseCase(ProductRepository products) { this.products = products; }

    @Transactional
    public Result<Void> execute(SetProductAvailabilityCommand cmd) {
        Optional<Product> maybe = products.findById(cmd.id());
        if (maybe.isEmpty()) {
            Notification n = Notification.empty();
            n.addError("PRODUCT_NOT_FOUND", "produto não encontrado");
            return Result.failure(n);
        }
        Product p = maybe.get();
        if (cmd.available()) p.markAvailable(); else p.markUnavailable();
        products.save(p);
        return Result.success(null);
    }
}
