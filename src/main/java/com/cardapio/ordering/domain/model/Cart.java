package com.cardapio.ordering.domain.model;

import com.cardapio.ordering.domain.exception.CartItemNotFoundException;
import com.cardapio.shared.domain.AggregateRoot;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Cart extends AggregateRoot<CartId> {

    private final UUID customerId;
    private final List<CartItem> items;
    private String couponCode;
    private final Instant createdAt;
    private Instant updatedAt;

    private Cart(CartId id, UUID customerId, List<CartItem> items, String couponCode,
                 Instant createdAt, Instant updatedAt) {
        super(id);
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "items"));
        this.couponCode = couponCode;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Cart createEmpty(UUID customerId, Clock clock) {
        Instant now = clock.instant();
        return new Cart(CartId.newId(), customerId, new ArrayList<>(), null, now, now);
    }

    public static Cart rehydrate(CartId id, UUID customerId, List<CartItem> items,
                                 Instant createdAt, Instant updatedAt) {
        return new Cart(id, customerId, items, null, createdAt, updatedAt);
    }

    public static Cart rehydrate(CartId id, UUID customerId, List<CartItem> items, String couponCode,
                                 Instant createdAt, Instant updatedAt) {
        return new Cart(id, customerId, items, couponCode, createdAt, updatedAt);
    }

    public void applyCoupon(String code, Clock clock) {
        this.couponCode = code;
        this.updatedAt = clock.instant();
    }

    public void removeCoupon(Clock clock) {
        if (couponCode != null) {
            this.couponCode = null;
            this.updatedAt = clock.instant();
        }
    }

    public Optional<String> couponCode() {
        return Optional.ofNullable(couponCode);
    }

    public CartItem addItem(UUID productId,
                            Optional<SelectedVariation> variation,
                            List<SelectedAddOn> addOns,
                            Optional<HalfAndHalf> halfAndHalf,
                            Observation observation,
                            int quantity,
                            Clock clock) {
        CartItem item = new CartItem(CartItemId.newId(), productId, variation, addOns, halfAndHalf, observation, quantity);
        items.add(item);
        updatedAt = clock.instant();
        return item;
    }

    public void updateItem(CartItemId itemId, int quantity, Observation observation, Clock clock) {
        CartItem item = items.stream()
            .filter(i -> i.id().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new CartItemNotFoundException(itemId));
        item.changeQuantity(quantity);
        item.replaceObservation(observation);
        updatedAt = clock.instant();
    }

    public void removeItem(CartItemId itemId, Clock clock) {
        boolean removed = items.removeIf(i -> i.id().equals(itemId));
        if (!removed) throw new CartItemNotFoundException(itemId);
        updatedAt = clock.instant();
    }

    public void clear(Clock clock) {
        items.clear();
        updatedAt = clock.instant();
    }

    public boolean isEmpty() { return items.isEmpty(); }
    public UUID customerId() { return customerId; }
    public List<CartItem> items() { return List.copyOf(items); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
