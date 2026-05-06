package com.cardapio.ordering.domain.exception;

import com.cardapio.ordering.domain.model.CartItemId;
import com.cardapio.shared.domain.DomainException;

public class CartItemNotFoundException extends DomainException {
    public CartItemNotFoundException(CartItemId id) {
        super("CART_ITEM_NOT_FOUND", "cart item not found: " + id.value());
    }
}
