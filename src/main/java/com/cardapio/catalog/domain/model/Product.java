package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.AggregateRoot;
import com.cardapio.shared.domain.Money;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Product extends AggregateRoot<ProductId> {

    private String name;
    private String description;
    private Money basePrice;
    private CategoryId categoryId;
    private String imageUrl;          // nullable
    private boolean available;
    private boolean allowsHalfHalf;
    private Stock stock;
    private final List<Variation> variations;
    private final List<AddOnGroup> addOnGroups;

    private Product(ProductId id, String name, String description, Money basePrice,
                    CategoryId categoryId, String imageUrl, boolean available, boolean allowsHalfHalf,
                    Stock stock, List<Variation> variations, List<AddOnGroup> addOnGroups) {
        super(id);
        rename(name);
        changeDescription(description);
        repriceBase(basePrice);
        moveToCategory(categoryId);
        this.imageUrl = imageUrl;
        this.available = available;
        this.allowsHalfHalf = allowsHalfHalf;
        this.stock = Objects.requireNonNull(stock, "stock");
        this.variations = new ArrayList<>(Objects.requireNonNull(variations, "variations"));
        this.addOnGroups = new ArrayList<>(Objects.requireNonNull(addOnGroups, "addOnGroups"));
    }

    public static Product create(String name, String description, Money basePrice,
                                 CategoryId categoryId, String imageUrl, boolean allowsHalfHalf) {
        return new Product(ProductId.newId(), name, description, basePrice, categoryId, imageUrl,
            true, allowsHalfHalf, Stock.untracked(), new ArrayList<>(), new ArrayList<>());
    }

    public static Product rehydrate(ProductId id, String name, String description, Money basePrice,
                                    CategoryId categoryId, String imageUrl, boolean available, boolean allowsHalfHalf,
                                    Stock stock, List<Variation> variations, List<AddOnGroup> addOnGroups) {
        return new Product(id, name, description, basePrice, categoryId, imageUrl, available, allowsHalfHalf,
            stock, variations, addOnGroups);
    }

    public void rename(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("product name must not be blank");
        this.name = trimmed;
    }

    public void changeDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public void repriceBase(Money basePrice) {
        this.basePrice = Objects.requireNonNull(basePrice, "basePrice");
    }

    public void moveToCategory(CategoryId categoryId) {
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
    }

    public void changeImage(String imageUrl) { this.imageUrl = imageUrl; }
    public void allowHalfHalf() { this.allowsHalfHalf = true; }
    public void disallowHalfHalf() { this.allowsHalfHalf = false; }

    public void markAvailable() { this.available = true; }
    public void markUnavailable() { this.available = false; }

    public void changeStock(Stock stock) { this.stock = Objects.requireNonNull(stock, "stock"); }

    public void addVariation(Variation v) { variations.add(Objects.requireNonNull(v)); }
    public void removeVariation(VariationId id) { variations.removeIf(v -> v.id().equals(id)); }

    public void addAddOnGroup(AddOnGroup g) { addOnGroups.add(Objects.requireNonNull(g)); }
    public void removeAddOnGroup(AddOnGroupId id) { addOnGroups.removeIf(g -> g.id().equals(id)); }

    public String name() { return name; }
    public String description() { return description; }
    public Money basePrice() { return basePrice; }
    public CategoryId categoryId() { return categoryId; }
    public String imageUrl() { return imageUrl; }
    public boolean isAvailable() { return available; }
    public boolean allowsHalfHalf() { return allowsHalfHalf; }
    public Stock stock() { return stock; }
    public List<Variation> variations() { return Collections.unmodifiableList(variations); }
    public List<AddOnGroup> addOnGroups() { return Collections.unmodifiableList(addOnGroups); }

    @Override
    public String toString() {
        return "Product{id=" + id() + ", name='" + name + "', available=" + available + "}";
    }
}
