package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.dto.CartView;
import com.cardapio.ordering.domain.model.Cart;
import com.cardapio.ordering.domain.port.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RemoveCouponUseCase {

    private final CartRepository carts;
    private final GetCartQuery getCart;
    private final Clock clock;

    @Transactional
    public CartView execute(UUID customerId) {
        Cart cart = carts.findByCustomerId(customerId).orElse(null);
        if (cart != null) {
            cart.removeCoupon(clock);
            carts.save(cart);
        }
        return getCart.getOrEmpty(customerId);
    }
}
