package com.cardapio.catalog.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class AddOnGroup {

    private final AddOnGroupId id;
    private String name;
    private int minSelection;
    private int maxSelection;
    private final List<AddOnItem> items;

    private AddOnGroup(AddOnGroupId id, String name, int minSelection, int maxSelection, List<AddOnItem> items) {
        this.id = Objects.requireNonNull(id, "id");
        rename(name);
        configureSelectionBounds(minSelection, maxSelection);
        this.items = new ArrayList<>(Objects.requireNonNull(items, "items"));
    }

    public static AddOnGroup create(String name, int minSelection, int maxSelection) {
        return new AddOnGroup(AddOnGroupId.newId(), name, minSelection, maxSelection, new ArrayList<>());
    }

    public static AddOnGroup rehydrate(AddOnGroupId id, String name, int minSelection, int maxSelection, List<AddOnItem> items) {
        return new AddOnGroup(id, name, minSelection, maxSelection, items);
    }

    public void rename(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("addon group name must not be blank");
        this.name = trimmed;
    }

    public void configureSelectionBounds(int min, int max) {
        if (min < 0) throw new IllegalArgumentException("min selection must be non-negative");
        if (max < min) throw new IllegalArgumentException("max selection must be >= min");
        this.minSelection = min;
        this.maxSelection = max;
    }

    public void addItem(AddOnItem item) { items.add(Objects.requireNonNull(item)); }

    public void removeItem(AddOnItemId itemId) {
        items.removeIf(i -> i.id().equals(itemId));
    }

    public AddOnGroupId id() { return id; }
    public String name() { return name; }
    public int minSelection() { return minSelection; }
    public int maxSelection() { return maxSelection; }
    public List<AddOnItem> items() { return Collections.unmodifiableList(items); }
}
