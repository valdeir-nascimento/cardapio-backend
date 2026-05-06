package com.cardapio.ordering.domain.exception;

import com.cardapio.shared.domain.DomainException;

import java.util.UUID;

public class CartNotFoundException extends DomainException {
    public CartNotFoundException(UUID customerId) {
        super("CART_NOT_FOUND", "cart not found for customer: " + customerId);
    }
}
