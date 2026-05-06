package com.cardapio.ordering.domain.model;

import com.cardapio.shared.domain.Money;

import java.util.Objects;
import java.util.UUID;

public record SelectedAddOn(UUID groupId, UUID itemId, String name, Money price, int quantity) {
    public SelectedAddOn {
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(price, "price");
        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if (quantity < 1) throw new IllegalArgumentException("quantity must be >= 1");
    }
}
