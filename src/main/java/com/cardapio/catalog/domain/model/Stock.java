package com.cardapio.catalog.domain.model;

public final class Stock {

    private final Integer quantity;  // null = untracked

    private Stock(Integer quantity) {
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("stock quantity must be non-negative");
        }
        this.quantity = quantity;
    }

    public static Stock untracked() { return new Stock(null); }
    public static Stock of(int quantity) { return new Stock(quantity); }

    public boolean isTracked() { return quantity != null; }
    public boolean isInStock() { return !isTracked() || quantity > 0; }
    public int quantity() {
        if (!isTracked()) throw new IllegalStateException("untracked stock has no quantity");
        return quantity;
    }
    public Integer rawQuantity() { return quantity; }  // null-safe accessor for persistence

    public Stock decrement(int amount) {
        if (!isTracked()) return this;
        if (amount < 0) throw new IllegalArgumentException("decrement must be non-negative");
        int next = quantity - amount;
        if (next < 0) throw new IllegalArgumentException("not enough stock");
        return new Stock(next);
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof Stock s)) return false;
        return java.util.Objects.equals(quantity, s.quantity);
    }
    @Override public int hashCode() { return java.util.Objects.hashCode(quantity); }
}
