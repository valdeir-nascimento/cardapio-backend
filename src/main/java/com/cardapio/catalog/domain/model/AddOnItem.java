package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.Money;

import java.util.Objects;

public final class AddOnItem {

    private final AddOnItemId id;
    private String name;
    private Money price;

    private AddOnItem(AddOnItemId id, String name, Money price) {
        this.id = Objects.requireNonNull(id, "id");
        rename(name);
        reprice(price);
    }

    public static AddOnItem create(String name, Money price) {
        return new AddOnItem(AddOnItemId.newId(), name, price);
    }

    public static AddOnItem rehydrate(AddOnItemId id, String name, Money price) {
        return new AddOnItem(id, name, price);
    }

    public void rename(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("addon item name must not be blank");
        this.name = trimmed;
    }

    public void reprice(Money price) { this.price = Objects.requireNonNull(price, "price"); }

    public AddOnItemId id() { return id; }
    public String name() { return name; }
    public Money price() { return price; }
}
