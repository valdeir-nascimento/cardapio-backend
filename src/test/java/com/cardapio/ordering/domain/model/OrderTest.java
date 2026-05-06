package com.cardapio.ordering.domain.model;

import com.cardapio.ordering.domain.exception.IllegalStatusTransitionException;
import com.cardapio.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-05T12:00:00Z"), ZoneOffset.UTC);
    private final UUID customer = UUID.randomUUID();

    private OrderItem simpleItem(int qty, String price) {
        return OrderItem.create(UUID.randomUUID(), "Pizza M", Money.brl(price),
            Optional.empty(), List.of(), Optional.empty(), Observation.empty(), qty);
    }

    private DeliveryAddress someAddress() {
        return new DeliveryAddress("Rua A", "100", "ap 1", "Centro", "Salvador", "40000-000", UUID.randomUUID());
    }

    @Test
    void placeComputesSubtotalAndTotalForDelivery() {
        Order o = Order.place(customer, OrderModality.DELIVERY,
            List.of(simpleItem(2, "10.00"), simpleItem(1, "5.00")),
            Money.brl("8.50"),
            Optional.of(someAddress()), clock);
        assertThat(o.subtotal().amount()).isEqualByComparingTo("25.00");
        assertThat(o.deliveryFee().amount()).isEqualByComparingTo("8.50");
        assertThat(o.total().amount()).isEqualByComparingTo("33.50");
        assertThat(o.status()).isEqualTo(OrderStatus.RECEIVED);
    }

    @Test
    void placePickupZeroFeeNoAddress() {
        Order o = Order.place(customer, OrderModality.PICKUP,
            List.of(simpleItem(1, "20.00")),
            Money.zeroBrl(),
            Optional.empty(), clock);
        assertThat(o.address()).isEmpty();
        assertThat(o.total().amount()).isEqualByComparingTo("20.00");
    }

    @Test
    void placeRejectsEmptyItems() {
        assertThatThrownBy(() -> Order.place(customer, OrderModality.PICKUP, List.of(),
            Money.zeroBrl(), Optional.empty(), clock))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeDeliveryRequiresAddress() {
        assertThatThrownBy(() -> Order.place(customer, OrderModality.DELIVERY,
            List.of(simpleItem(1, "10")), Money.brl("5"), Optional.empty(), clock))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placePickupRejectsPositiveFee() {
        assertThatThrownBy(() -> Order.place(customer, OrderModality.PICKUP,
            List.of(simpleItem(1, "10")), Money.brl("5"), Optional.empty(), clock))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deliveryHappyPathTransitions() {
        Order o = Order.place(customer, OrderModality.DELIVERY,
            List.of(simpleItem(1, "10")), Money.brl("5"), Optional.of(someAddress()), clock);
        o.advance(OrderStatus.CONFIRMED, clock);
        o.advance(OrderStatus.PREPARING, clock);
        o.advance(OrderStatus.READY, clock);
        o.advance(OrderStatus.OUT_FOR_DELIVERY, clock);
        o.advance(OrderStatus.DELIVERED, clock);
        assertThat(o.status()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void invalidTransitionThrows() {
        Order o = Order.place(customer, OrderModality.DELIVERY,
            List.of(simpleItem(1, "10")), Money.brl("5"), Optional.of(someAddress()), clock);
        assertThatThrownBy(() -> o.advance(OrderStatus.PREPARING, clock))
            .isInstanceOf(IllegalStatusTransitionException.class);
    }

    @Test
    void cancelAfterPreparingThrows() {
        Order o = Order.place(customer, OrderModality.DELIVERY,
            List.of(simpleItem(1, "10")), Money.brl("5"), Optional.of(someAddress()), clock);
        o.advance(OrderStatus.CONFIRMED, clock);
        o.advance(OrderStatus.PREPARING, clock);
        assertThatThrownBy(() -> o.cancel(clock))
            .isInstanceOf(IllegalStatusTransitionException.class);
    }

    @Test
    void cancelFromReceivedOk() {
        Order o = Order.place(customer, OrderModality.DELIVERY,
            List.of(simpleItem(1, "10")), Money.brl("5"), Optional.of(someAddress()), clock);
        o.cancel(clock);
        assertThat(o.status()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void halfAndHalfUsesHigherBase() {
        OrderItem item = OrderItem.create(UUID.randomUUID(), "Pizza Half", Money.brl("30.00"),
            Optional.empty(), List.of(),
            Optional.of(new HalfAndHalf(UUID.randomUUID(), UUID.randomUUID(), "Calabresa+Margherita", Money.brl("40.00"))),
            Observation.empty(), 1);
        assertThat(item.lineTotal().amount()).isEqualByComparingTo("40.00");
    }

    @Test
    void orderItemAppliesVariationAndAddons() {
        OrderItem item = OrderItem.create(UUID.randomUUID(), "Pizza M", Money.brl("30.00"),
            Optional.of(new SelectedVariation(UUID.randomUUID(), "G", Money.brl("10.00"))),
            List.of(new SelectedAddOn(UUID.randomUUID(), UUID.randomUUID(), "Bacon", Money.brl("3.50"), 2)),
            Optional.empty(), Observation.empty(), 2);
        // (30 + 10 + (3.50*2)) * 2 = 47 * 2 = 94
        assertThat(item.lineTotal().amount()).isEqualByComparingTo("94.00");
    }
}
