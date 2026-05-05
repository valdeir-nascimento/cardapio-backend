package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.SetProductStockCommand;
import com.cardapio.catalog.domain.model.Stock;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetProductStockUseCase extends WriteProductUseCase {

    public SetProductStockUseCase(ProductRepository products) {
        super(products);
    }

    @Transactional
    public Result<Void> execute(SetProductStockCommand cmd) {
        if (cmd.quantity() != null && cmd.quantity() < 0) {
            return Result.failWith(ErrorCode.INVALID_STOCK);
        }
        return loadProduct(cmd.id()).flatMap(p -> {
            p.changeStock(cmd.quantity() == null ? Stock.untracked() : Stock.of(cmd.quantity()));
            products.save(p);
            return Result.ok();
        });
    }
}
