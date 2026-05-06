package com.cardapio.ordering.domain.model;

import com.cardapio.ordering.domain.exception.CartItemNotFoundException;
import com.cardapio.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-05T12:00:00Z"), ZoneOffset.UTC);
    private final UUID customer = UUID.randomUUID();
    private final UUID product = UUID.randomUUID();

    @Test
    void addItemAppendsToList() {
        Cart cart = Cart.createEmpty(customer, clock);
        cart.addItem(product, Optional.empty(), List.of(), Optional.empty(), Observation.empty(), 2, clock);
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void updateItemChangesQuantityAndObservation() {
        Cart cart = Cart.createEmpty(customer, clock);
        CartItem item = cart.addItem(product, Optional.empty(), List.of(), Optional.empty(), Observation.empty(), 1, clock);
        cart.updateItem(item.id(), 5, Observation.of("sem cebola"), clock);
        assertThat(cart.items().get(0).quantity()).isEqualTo(5);
        assertThat(cart.items().get(0).observation().value()).isEqualTo("sem cebola");
    }

    @Test
    void removeItemDeletes() {
        Cart cart = Cart.createEmpty(customer, clock);
        CartItem item = cart.addItem(product, Optional.empty(), List.of(), Optional.empty(), Observation.empty(), 1, clock);
        cart.removeItem(item.id(), clock);
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void removeMissingThrows() {
        Cart cart = Cart.createEmpty(customer, clock);
        assertThatThrownBy(() -> cart.removeItem(CartItemId.newId(), clock))
            .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void updateMissingThrows() {
        Cart cart = Cart.createEmpty(customer, clock);
        assertThatThrownBy(() -> cart.updateItem(CartItemId.newId(), 1, Observation.empty(), clock))
            .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    void clearEmptiesItems() {
        Cart cart = Cart.createEmpty(customer, clock);
        cart.addItem(product, Optional.empty(), List.of(), Optional.empty(), Observation.empty(), 1, clock);
        cart.clear(clock);
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void cartItemRejectsVariationAndHalfHalfTogether() {
        SelectedVariation v = new SelectedVariation(UUID.randomUUID(), "M", Money.brl("2"));
        HalfAndHalf hh = new HalfAndHalf(UUID.randomUUID(), UUID.randomUUID(), "Half", Money.brl("20"));
        assertThatThrownBy(() -> new CartItem(CartItemId.newId(), product, Optional.of(v), List.of(), Optional.of(hh),
            Observation.empty(), 1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
