package com.cardapio.ordering.domain.model;

import com.cardapio.ordering.domain.exception.DineInInvariantException;
import com.cardapio.ordering.domain.exception.IllegalStatusTransitionException;
import com.cardapio.shared.domain.AggregateRoot;
import com.cardapio.shared.domain.Money;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Order extends AggregateRoot<OrderId> {

    private final UUID customerId;
    private final OrderModality modality;
    private OrderStatus status;
    private final List<OrderItem> items;
    private final Money subtotal;
    private final Money deliveryFee;
    private final Money discount;
    private final Money total;
    private final DeliveryAddress address;
    private final TableId tableId;
    private final ComandaId comandaId;
    private final Instant placedAt;
    private Instant updatedAt;

    private Order(OrderId id, UUID customerId, OrderModality modality, OrderStatus status,
                  List<OrderItem> items, Money subtotal, Money deliveryFee, Money discount, Money total,
                  Optional<DeliveryAddress> address, Optional<TableId> tableId, Optional<ComandaId> comandaId,
                  Instant placedAt, Instant updatedAt) {
        super(id);
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.modality = Objects.requireNonNull(modality, "modality");
        this.status = Objects.requireNonNull(status, "status");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "items"));
        this.subtotal = Objects.requireNonNull(subtotal, "subtotal");
        this.deliveryFee = Objects.requireNonNull(deliveryFee, "deliveryFee");
        this.discount = Objects.requireNonNull(discount, "discount");
        this.total = Objects.requireNonNull(total, "total");
        this.address = address.orElse(null);
        this.tableId = tableId.orElse(null);
        this.comandaId = comandaId.orElse(null);
        this.placedAt = Objects.requireNonNull(placedAt, "placedAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        enforceModalityInvariants();
    }

    private void enforceModalityInvariants() {
        if (modality == OrderModality.DINE_IN) {
            if (tableId == null || comandaId == null) {
                throw new DineInInvariantException("DINE_IN order requires tableId and comandaId");
            }
            if (address != null) {
                throw new DineInInvariantException("DINE_IN order must not have a delivery address");
            }
            if (deliveryFee.amount().signum() != 0) {
                throw new DineInInvariantException("DINE_IN order must have zero delivery fee");
            }
        } else {
            if (tableId != null || comandaId != null) {
                throw new DineInInvariantException("only DINE_IN orders may carry tableId/comandaId");
            }
        }
    }

    public static Order place(UUID customerId, OrderModality modality, List<OrderItem> items,
                              Money deliveryFee, Optional<DeliveryAddress> address, Clock clock) {
        return place(customerId, modality, items, deliveryFee, address, Optional.empty(), Optional.empty(), clock);
    }

    public static Order place(UUID customerId, OrderModality modality, List<OrderItem> items,
                              Money deliveryFee, Optional<DeliveryAddress> address,
                              Optional<TableId> tableId, Optional<ComandaId> comandaId, Clock clock) {
        if (items.isEmpty()) throw new IllegalArgumentException("order has no items");
        if (modality == OrderModality.DELIVERY && address.isEmpty()) {
            throw new IllegalArgumentException("delivery requires address");
        }
        if (modality == OrderModality.PICKUP && deliveryFee.amount().signum() > 0) {
            throw new IllegalArgumentException("pickup must have zero fee");
        }
        Currency currency = items.get(0).lineTotal().currency();
        Money zero = Money.of(java.math.BigDecimal.ZERO, currency);
        Money subtotal = items.stream().map(OrderItem::lineTotal).reduce(zero, Money::add);
        Money total = subtotal.add(deliveryFee);
        Instant now = clock.instant();
        return new Order(OrderId.newId(), customerId, modality, OrderStatus.RECEIVED,
            items, subtotal, deliveryFee, zero, total, address, tableId, comandaId, now, now);
    }

    public static Order rehydrate(OrderId id, UUID customerId, OrderModality modality, OrderStatus status,
                                  List<OrderItem> items, Money subtotal, Money deliveryFee, Money discount, Money total,
                                  Optional<DeliveryAddress> address, Instant placedAt, Instant updatedAt) {
        return rehydrate(id, customerId, modality, status, items, subtotal, deliveryFee, discount, total,
            address, Optional.empty(), Optional.empty(), placedAt, updatedAt);
    }

    public static Order rehydrate(OrderId id, UUID customerId, OrderModality modality, OrderStatus status,
                                  List<OrderItem> items, Money subtotal, Money deliveryFee, Money discount, Money total,
                                  Optional<DeliveryAddress> address, Optional<TableId> tableId, Optional<ComandaId> comandaId,
                                  Instant placedAt, Instant updatedAt) {
        return new Order(id, customerId, modality, status, items, subtotal, deliveryFee, discount, total,
            address, tableId, comandaId, placedAt, updatedAt);
    }

    public void advance(OrderStatus next, Clock clock) {
        if (!status.canTransitionTo(next, modality)) {
            throw new IllegalStatusTransitionException(status, next, modality);
        }
        this.status = next;
        this.updatedAt = clock.instant();
    }

    public void cancel(Clock clock) {
        if (!status.canTransitionTo(OrderStatus.CANCELED, modality)) {
            throw new IllegalStatusTransitionException(status, OrderStatus.CANCELED, modality);
        }
        this.status = OrderStatus.CANCELED;
        this.updatedAt = clock.instant();
    }

    public UUID customerId() { return customerId; }
    public OrderModality modality() { return modality; }
    public OrderStatus status() { return status; }
    public List<OrderItem> items() { return List.copyOf(items); }
    public Money subtotal() { return subtotal; }
    public Money deliveryFee() { return deliveryFee; }
    public Money discount() { return discount; }
    public Money total() { return total; }
    public Optional<DeliveryAddress> address() { return Optional.ofNullable(address); }
    public Optional<TableId> tableId() { return Optional.ofNullable(tableId); }
    public Optional<ComandaId> comandaId() { return Optional.ofNullable(comandaId); }
    public Instant placedAt() { return placedAt; }
    public Instant updatedAt() { return updatedAt; }
}
