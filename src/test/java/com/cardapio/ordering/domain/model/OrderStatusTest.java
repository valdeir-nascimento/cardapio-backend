package com.cardapio.ordering.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void deliveryHappyPathTransitions() {
        assertThat(OrderStatus.RECEIVED.canTransitionTo(OrderStatus.CONFIRMED, OrderModality.DELIVERY)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PREPARING, OrderModality.DELIVERY)).isTrue();
        assertThat(OrderStatus.PREPARING.canTransitionTo(OrderStatus.READY, OrderModality.DELIVERY)).isTrue();
        assertThat(OrderStatus.READY.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY, OrderModality.DELIVERY)).isTrue();
        assertThat(OrderStatus.OUT_FOR_DELIVERY.canTransitionTo(OrderStatus.DELIVERED, OrderModality.DELIVERY)).isTrue();
    }

    @Test
    void pickupHappyPathTransitions() {
        assertThat(OrderStatus.RECEIVED.canTransitionTo(OrderStatus.CONFIRMED, OrderModality.PICKUP)).isTrue();
        assertThat(OrderStatus.READY.canTransitionTo(OrderStatus.PICKED_UP, OrderModality.PICKUP)).isTrue();
        assertThat(OrderStatus.READY.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY, OrderModality.PICKUP)).isFalse();
    }

    @Test
    void cannotSkipStates() {
        assertThat(OrderStatus.RECEIVED.canTransitionTo(OrderStatus.PREPARING, OrderModality.DELIVERY)).isFalse();
        assertThat(OrderStatus.PREPARING.canTransitionTo(OrderStatus.DELIVERED, OrderModality.DELIVERY)).isFalse();
    }

    @Test
    void cancelOnlyFromEarlyStates() {
        assertThat(OrderStatus.RECEIVED.canTransitionTo(OrderStatus.CANCELED, OrderModality.DELIVERY)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELED, OrderModality.DELIVERY)).isTrue();
        assertThat(OrderStatus.PREPARING.canTransitionTo(OrderStatus.CANCELED, OrderModality.DELIVERY)).isFalse();
    }

    @Test
    void terminalStatesAreTerminal() {
        assertThat(OrderStatus.DELIVERED.isTerminal()).isTrue();
        assertThat(OrderStatus.PICKED_UP.isTerminal()).isTrue();
        assertThat(OrderStatus.CANCELED.isTerminal()).isTrue();
        assertThat(OrderStatus.RECEIVED.isTerminal()).isFalse();
        assertThat(OrderStatus.PREPARING.isTerminal()).isFalse();
    }

    @Test
    void terminalStatesAllowNoTransitions() {
        for (OrderStatus s : new OrderStatus[]{OrderStatus.DELIVERED, OrderStatus.PICKED_UP, OrderStatus.CANCELED}) {
            for (OrderStatus target : OrderStatus.values()) {
                assertThat(s.canTransitionTo(target, OrderModality.DELIVERY)).isFalse();
                assertThat(s.canTransitionTo(target, OrderModality.PICKUP)).isFalse();
            }
        }
    }
}
