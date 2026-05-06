package com.cardapio.ordering.domain.port;

import com.cardapio.ordering.domain.model.Cart;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository {
    Optional<Cart> findByCustomerId(UUID customerId);
    void save(Cart cart);
    void delete(Cart cart);
}
