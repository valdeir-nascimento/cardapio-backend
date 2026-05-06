package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.command.RemoveCartItemCommand;
import com.cardapio.ordering.domain.exception.CartItemNotFoundException;
import com.cardapio.ordering.domain.model.Cart;
import com.cardapio.ordering.domain.model.CartItemId;
import com.cardapio.ordering.domain.port.CartRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class RemoveCartItemUseCase {

    private final CartRepository cartRepo;
    private final Clock clock;

    @Transactional
    public Result<Void> execute(RemoveCartItemCommand cmd) {
        Cart cart = cartRepo.findByCustomerId(cmd.customerId()).orElse(null);
        if (cart == null) return Result.failWith(ErrorCode.CART_ITEM_NOT_FOUND);

        try {
            cart.removeItem(CartItemId.of(cmd.cartItemId()), clock);
        } catch (CartItemNotFoundException e) {
            return Result.failWith(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        cartRepo.save(cart);
        return Result.ok();
    }
}
