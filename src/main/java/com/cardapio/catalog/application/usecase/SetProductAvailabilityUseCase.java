package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.SetProductAvailabilityCommand;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetProductAvailabilityUseCase extends WriteProductUseCase {

    public SetProductAvailabilityUseCase(ProductRepository products) {
        super(products);
    }

    @Transactional
    public Result<Void> execute(SetProductAvailabilityCommand cmd) {
        return loadProduct(cmd.id()).flatMap(p -> {
            if (cmd.available()) p.markAvailable(); else p.markUnavailable();
            products.save(p);
            return Result.ok();
        });
    }
}
