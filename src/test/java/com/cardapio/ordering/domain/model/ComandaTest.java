package com.cardapio.ordering.domain.model;

import com.cardapio.ordering.domain.event.ComandaClosed;
import com.cardapio.ordering.domain.event.ComandaJoined;
import com.cardapio.ordering.domain.event.ComandaOpened;
import com.cardapio.ordering.domain.exception.DineInInvariantException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComandaTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T12:00:00Z"), ZoneOffset.UTC);
    private final TableId tableId = TableId.newId();

    @Test
    void openInitializesAndEmitsEvent() {
        UUID opener = UUID.randomUUID();
        Comanda comanda = Comanda.open(tableId, opener, clock);

        assertThat(comanda.status()).isEqualTo(ComandaStatus.OPEN);
        assertThat(comanda.tableId()).isEqualTo(tableId);
        assertThat(comanda.customerIds()).containsExactly(opener);
        assertThat(comanda.orderIds()).isEmpty();
        assertThat(comanda.closedAt()).isNull();
        assertThat(comanda.pullDomainEvents())
            .singleElement()
            .isInstanceOf(ComandaOpened.class);
    }

    @Test
    void joinAddsNewCustomerAndEmitsEvent() {
        Comanda comanda = Comanda.open(tableId, UUID.randomUUID(), clock);
        comanda.pullDomainEvents();
        UUID newCustomer = UUID.randomUUID();

        comanda.join(newCustomer, clock);

        assertThat(comanda.customerIds()).contains(newCustomer);
        assertThat(comanda.pullDomainEvents())
            .singleElement()
            .isInstanceOf(ComandaJoined.class);
    }

    @Test
    void joinIsIdempotent() {
        UUID opener = UUID.randomUUID();
        Comanda comanda = Comanda.open(tableId, opener, clock);
        comanda.pullDomainEvents();

        comanda.join(opener, clock);

        assertThat(comanda.customerIds()).hasSize(1);
        assertThat(comanda.pullDomainEvents()).isEmpty();
    }

    @Test
    void joinAfterCloseThrows() {
        Comanda comanda = Comanda.open(tableId, UUID.randomUUID(), clock);
        comanda.close(clock);

        assertThatThrownBy(() -> comanda.join(UUID.randomUUID(), clock))
            .isInstanceOf(DineInInvariantException.class);
    }

    @Test
    void attachOrderAppendsAndIsIdempotent() {
        Comanda comanda = Comanda.open(tableId, UUID.randomUUID(), clock);
        OrderId orderId = OrderId.newId();

        comanda.attachOrder(orderId, clock);
        comanda.attachOrder(orderId, clock);

        assertThat(comanda.orderIds()).containsExactly(orderId);
    }

    @Test
    void attachOrderAfterCloseThrows() {
        Comanda comanda = Comanda.open(tableId, UUID.randomUUID(), clock);
        comanda.close(clock);

        assertThatThrownBy(() -> comanda.attachOrder(OrderId.newId(), clock))
            .isInstanceOf(DineInInvariantException.class);
    }

    @Test
    void closeTransitionsAndEmitsEvent() {
        Comanda comanda = Comanda.open(tableId, UUID.randomUUID(), clock);
        comanda.pullDomainEvents();

        comanda.close(clock);

        assertThat(comanda.status()).isEqualTo(ComandaStatus.CLOSED);
        assertThat(comanda.closedAt()).isEqualTo(clock.instant());
        assertThat(comanda.pullDomainEvents())
            .singleElement()
            .isInstanceOf(ComandaClosed.class);
    }

    @Test
    void closeIsIdempotent() {
        Comanda comanda = Comanda.open(tableId, UUID.randomUUID(), clock);
        comanda.close(clock);
        comanda.pullDomainEvents();

        comanda.close(clock);

        assertThat(comanda.status()).isEqualTo(ComandaStatus.CLOSED);
        assertThat(comanda.pullDomainEvents()).isEmpty();
    }

    @Test
    void hasMemberReportsMembership() {
        UUID opener = UUID.randomUUID();
        Comanda comanda = Comanda.open(tableId, opener, clock);

        assertThat(comanda.hasMember(opener)).isTrue();
        assertThat(comanda.hasMember(UUID.randomUUID())).isFalse();
    }
}
