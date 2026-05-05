package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.Money;

import java.util.Objects;

public final class Variation {

    private final VariationId id;
    private String name;
    private Money priceModifier;  // can be 0; added to product basePrice

    private Variation(VariationId id, String name, Money priceModifier) {
        this.id = Objects.requireNonNull(id, "id");
        rename(name);
        repriceBy(priceModifier);
    }

    public static Variation create(String name, Money priceModifier) {
        return new Variation(VariationId.newId(), name, priceModifier);
    }

    public static Variation rehydrate(VariationId id, String name, Money priceModifier) {
        return new Variation(id, name, priceModifier);
    }

    public void rename(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("variation name must not be blank");
        this.name = trimmed;
    }

    public void repriceBy(Money priceModifier) {
        this.priceModifier = Objects.requireNonNull(priceModifier, "priceModifier");
    }

    public VariationId id() { return id; }
    public String name() { return name; }
    public Money priceModifier() { return priceModifier; }
}
