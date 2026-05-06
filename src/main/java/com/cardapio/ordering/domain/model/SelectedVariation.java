package com.cardapio.ordering.domain.model;

import com.cardapio.shared.domain.Money;

import java.util.Objects;
import java.util.UUID;

public record SelectedVariation(UUID variationId, String name, Money priceModifier) {
    public SelectedVariation {
        Objects.requireNonNull(variationId, "variationId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(priceModifier, "priceModifier");
        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
    }
}
