package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.AggregateRoot;

import java.util.Objects;

public final class Category extends AggregateRoot<CategoryId> {

    private String name;
    private int displayOrder;
    private boolean active;

    private Category(CategoryId id, String name, int displayOrder, boolean active) {
        super(id);
        setName(name);
        setDisplayOrder(displayOrder);
        this.active = active;
    }

    public static Category create(String name, int displayOrder) {
        return new Category(CategoryId.newId(), name, displayOrder, true);
    }

    public static Category rehydrate(CategoryId id, String name, int displayOrder, boolean active) {
        return new Category(id, name, displayOrder, active);
    }

    public void rename(String name) { setName(name); }
    public void reorder(int displayOrder) { setDisplayOrder(displayOrder); }
    public void activate() { this.active = true; }
    public void deactivate() { this.active = false; }

    private void setName(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("name must not be blank");
        this.name = trimmed;
    }

    private void setDisplayOrder(int order) {
        if (order < 0) throw new IllegalArgumentException("displayOrder must be non-negative");
        this.displayOrder = order;
    }

    public String name() { return name; }
    public int displayOrder() { return displayOrder; }
    public boolean isActive() { return active; }

    @Override
    public String toString() {
        return "Category{id=" + id() + ", name='" + name + "', active=" + active + "}";
    }
}
