package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.command.AddCartItemCommand;
import com.cardapio.ordering.domain.model.Cart;
import com.cardapio.ordering.domain.model.CartItem;
import com.cardapio.ordering.domain.port.CartRepository;
import com.cardapio.ordering.domain.port.CatalogQueryPort;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddCartItemUseCase {

    private final CartRepository cartRepo;
    private final CatalogQueryPort catalog;
    private final Clock clock;

    @Transactional
    public Result<UUID> execute(AddCartItemCommand cmd) {
        Result<CartItemFactory.Built> built = CartItemFactory.build(cmd, catalog);
        if (!built.isSuccess()) return Result.failure(((Result.Failure<CartItemFactory.Built>) built).notification());
        CartItemFactory.Built b = ((Result.Success<CartItemFactory.Built>) built).value();

        Cart cart = cartRepo.findByCustomerId(cmd.customerId())
            .orElseGet(() -> Cart.createEmpty(cmd.customerId(), clock));

        CartItem item = cart.addItem(b.productId(), b.variation(), b.addOns(), b.halfAndHalf(),
            b.observation(), b.quantity(), clock);
        cartRepo.save(cart);
        return Result.success(item.id().value());
    }
}
