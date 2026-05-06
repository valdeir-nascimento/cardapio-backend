package com.cardapio.ordering.domain.model;

import com.cardapio.shared.domain.Money;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class OrderItem {

    private final OrderItemId id;
    private final UUID productId;
    private final String productName;
    private final SelectedVariation variation;
    private final List<SelectedAddOn> addOns;
    private final HalfAndHalf halfAndHalf;
    private final Observation observation;
    private final int quantity;
    private final Money lineTotal;

    private OrderItem(OrderItemId id, UUID productId, String productName,
                      SelectedVariation variation, List<SelectedAddOn> addOns,
                      HalfAndHalf halfAndHalf, Observation observation,
                      int quantity, Money lineTotal) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.variation = variation;
        this.addOns = List.copyOf(addOns);
        this.halfAndHalf = halfAndHalf;
        this.observation = observation;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    /**
     * Builds an OrderItem with frozen pricing snapshot.
     * lineTotal = (effectiveBase + variationModifier + sum(addOnPrice * addOnQty)) * itemQuantity
     * For half-and-half: effective base is the higher of the two halves' base prices.
     */
    public static OrderItem create(UUID productId, String productName,
                                   Money productBasePrice,
                                   Optional<SelectedVariation> variation,
                                   List<SelectedAddOn> addOns,
                                   Optional<HalfAndHalf> halfAndHalf,
                                   Observation observation,
                                   int quantity) {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(productName, "productName");
        Objects.requireNonNull(productBasePrice, "productBasePrice");
        Objects.requireNonNull(variation, "variation");
        Objects.requireNonNull(addOns, "addOns");
        Objects.requireNonNull(halfAndHalf, "halfAndHalf");
        Objects.requireNonNull(observation, "observation");
        if (quantity < 1) throw new IllegalArgumentException("quantity must be >= 1");
        if (variation.isPresent() && halfAndHalf.isPresent()) {
            throw new IllegalArgumentException("variation and halfAndHalf are mutually exclusive");
        }

        Money base = halfAndHalf
            .map(hh -> hh.basePrice().amount().compareTo(productBasePrice.amount()) >= 0
                ? hh.basePrice() : productBasePrice)
            .orElse(productBasePrice);

        Money unitPrice = base;
        if (variation.isPresent()) {
            unitPrice = unitPrice.add(variation.get().priceModifier());
        }
        for (SelectedAddOn addOn : addOns) {
            unitPrice = unitPrice.add(addOn.price().multiply(addOn.quantity()));
        }
        Money line = unitPrice.multiply(quantity);

        return new OrderItem(OrderItemId.newId(), productId, productName,
            variation.orElse(null), addOns, halfAndHalf.orElse(null),
            observation, quantity, line);
    }

    public static OrderItem rehydrate(OrderItemId id, UUID productId, String productName,
                                      Optional<SelectedVariation> variation, List<SelectedAddOn> addOns,
                                      Optional<HalfAndHalf> halfAndHalf, Observation observation,
                                      int quantity, Money lineTotal) {
        return new OrderItem(id, productId, productName, variation.orElse(null), addOns,
            halfAndHalf.orElse(null), observation, quantity, lineTotal);
    }

    public OrderItemId id() { return id; }
    public UUID productId() { return productId; }
    public String productName() { return productName; }
    public Optional<SelectedVariation> variation() { return Optional.ofNullable(variation); }
    public List<SelectedAddOn> addOns() { return addOns; }
    public Optional<HalfAndHalf> halfAndHalf() { return Optional.ofNullable(halfAndHalf); }
    public Observation observation() { return observation; }
    public int quantity() { return quantity; }
    public Money lineTotal() { return lineTotal; }
}
