package com.cardapio.ordering.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CartItem {

    private final CartItemId id;
    private final UUID productId;
    private final SelectedVariation variation;
    private final List<SelectedAddOn> addOns;
    private final HalfAndHalf halfAndHalf;
    private Observation observation;
    private int quantity;

    public CartItem(CartItemId id,
                    UUID productId,
                    Optional<SelectedVariation> variation,
                    List<SelectedAddOn> addOns,
                    Optional<HalfAndHalf> halfAndHalf,
                    Observation observation,
                    int quantity) {
        this.id = Objects.requireNonNull(id, "id");
        this.productId = Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(variation, "variation");
        Objects.requireNonNull(addOns, "addOns");
        Objects.requireNonNull(halfAndHalf, "halfAndHalf");
        this.variation = variation.orElse(null);
        this.addOns = List.copyOf(addOns);
        this.halfAndHalf = halfAndHalf.orElse(null);
        this.observation = Objects.requireNonNull(observation, "observation");
        if (quantity < 1) throw new IllegalArgumentException("quantity must be >= 1");
        if (this.variation != null && this.halfAndHalf != null) {
            throw new IllegalArgumentException("variation and halfAndHalf are mutually exclusive");
        }
        this.quantity = quantity;
    }

    public void changeQuantity(int newQuantity) {
        if (newQuantity < 1) throw new IllegalArgumentException("quantity must be >= 1");
        this.quantity = newQuantity;
    }

    public void replaceObservation(Observation newObservation) {
        this.observation = Objects.requireNonNull(newObservation, "observation");
    }

    public CartItemId id() { return id; }
    public UUID productId() { return productId; }
    public Optional<SelectedVariation> variation() { return Optional.ofNullable(variation); }
    public List<SelectedAddOn> addOns() { return addOns; }
    public Optional<HalfAndHalf> halfAndHalf() { return Optional.ofNullable(halfAndHalf); }
    public Observation observation() { return observation; }
    public int quantity() { return quantity; }
}
