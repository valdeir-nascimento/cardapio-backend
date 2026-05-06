package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.command.UpdateCartItemCommand;
import com.cardapio.ordering.domain.exception.CartItemNotFoundException;
import com.cardapio.ordering.domain.model.Cart;
import com.cardapio.ordering.domain.model.CartItemId;
import com.cardapio.ordering.domain.model.Observation;
import com.cardapio.ordering.domain.port.CartRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCartItemUseCase {

    private final CartRepository cartRepo;
    private final java.time.Clock clock;

    @Transactional
    public Result<Void> execute(UpdateCartItemCommand cmd) {
        if (cmd.quantity() < 1) return Result.failWith(ErrorCode.INVALID_QUANTITY);

        Cart cart = cartRepo.findByCustomerId(cmd.customerId()).orElse(null);
        if (cart == null) return Result.failWith(ErrorCode.CART_ITEM_NOT_FOUND);

        try {
            cart.updateItem(CartItemId.of(cmd.cartItemId()), cmd.quantity(), Observation.of(cmd.observation()), clock);
        } catch (CartItemNotFoundException e) {
            return Result.failWith(ErrorCode.CART_ITEM_NOT_FOUND);
        } catch (IllegalArgumentException e) {
            return Result.failWith(ErrorCode.INVALID_OBSERVATION, e.getMessage());
        }
        cartRepo.save(cart);
        return Result.ok();
    }
}
