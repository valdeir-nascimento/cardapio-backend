package com.cardapio.ordering.infrastructure.persistence.adapter;

import com.cardapio.ordering.domain.model.Cart;
import com.cardapio.ordering.domain.port.CartRepository;
import com.cardapio.ordering.infrastructure.persistence.mapper.CartMapper;
import com.cardapio.ordering.infrastructure.persistence.repository.SpringCartJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CartRepositoryAdapter implements CartRepository {

    private final SpringCartJpaRepository jpa;

    @Override
    public Optional<Cart> findByCustomerId(UUID customerId) {
        return jpa.findByCustomerId(customerId).map(CartMapper::toDomain);
    }

    @Override
    public void save(Cart cart) {
        var existing = jpa.findById(cart.id().value());
        if (existing.isPresent()) {
            CartMapper.rebuildItems(existing.get(), cart);
            jpa.save(existing.get());
        } else {
            jpa.save(CartMapper.toJpa(cart));
        }
    }

    @Override
    public void delete(Cart cart) {
        jpa.deleteById(cart.id().value());
    }
}
