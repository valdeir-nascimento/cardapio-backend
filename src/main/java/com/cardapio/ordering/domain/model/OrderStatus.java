package com.cardapio.ordering.domain.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    RECEIVED, CONFIRMED, PREPARING, READY, OUT_FOR_DELIVERY, PICKED_UP, DELIVERED, CANCELED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_DELIVERY = Map.of(
        RECEIVED,         EnumSet.of(CONFIRMED, CANCELED),
        CONFIRMED,        EnumSet.of(PREPARING, CANCELED),
        PREPARING,        EnumSet.of(READY),
        READY,            EnumSet.of(OUT_FOR_DELIVERY),
        OUT_FOR_DELIVERY, EnumSet.of(DELIVERED),
        DELIVERED,        EnumSet.noneOf(OrderStatus.class),
        CANCELED,         EnumSet.noneOf(OrderStatus.class)
    );

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_PICKUP = Map.of(
        RECEIVED,  EnumSet.of(CONFIRMED, CANCELED),
        CONFIRMED, EnumSet.of(PREPARING, CANCELED),
        PREPARING, EnumSet.of(READY),
        READY,     EnumSet.of(PICKED_UP),
        PICKED_UP, EnumSet.noneOf(OrderStatus.class),
        CANCELED,  EnumSet.noneOf(OrderStatus.class)
    );

    public boolean canTransitionTo(OrderStatus target, OrderModality modality) {
        Map<OrderStatus, Set<OrderStatus>> table = switch (modality) {
            case DELIVERY -> ALLOWED_DELIVERY;
            case PICKUP   -> ALLOWED_PICKUP;
        };
        return table.getOrDefault(this, EnumSet.noneOf(OrderStatus.class)).contains(target);
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == PICKED_UP || this == CANCELED;
    }
}
