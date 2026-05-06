package com.cardapio.notification.infrastructure.ordering;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.notification.domain.port.OrderSummaryPort;
import com.cardapio.shared.domain.Notification;
import com.cardapio.ordering.application.OrderingFacade;
import com.cardapio.ordering.application.dto.OrderView;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderSummaryAdapterTest {

    @Test
    void mapsOrderViewToSummary() {
        OrderingFacade facade = mock(OrderingFacade.class);
        UUID orderId = UUID.fromString("11112222-3333-4444-5555-666677778888");
        UUID customerId = UUID.randomUUID();
        OrderView view = new OrderView(
            orderId, customerId, OrderModality.DELIVERY, OrderStatus.CONFIRMED,
            List.of(),
            new BigDecimal("40.00"),
            new BigDecimal("5.00"),
            BigDecimal.ZERO,
            new BigDecimal("45.00"),
            "BRL",
            null,
            Instant.parse("2026-05-06T10:00:00Z"),
            Instant.parse("2026-05-06T10:01:00Z")
        );
        when(facade.getOrderAdmin(any())).thenReturn(view);

        OrderSummaryAdapter adapter = new OrderSummaryAdapter(facade);
        Optional<OrderSummaryPort.OrderSummary> result = adapter.find(orderId);

        assertThat(result).isPresent();
        assertThat(result.get().shortRef()).isEqualTo("111122");
        assertThat(result.get().total()).isEqualByComparingTo("45.00");
        assertThat(result.get().modality()).isEqualTo(OrderModality.DELIVERY);
        assertThat(result.get().customerId()).isEqualTo(customerId);
    }

    @Test
    void returnsEmptyOnNotFound() {
        OrderingFacade facade = mock(OrderingFacade.class);
        when(facade.getOrderAdmin(any(OrderId.class))).thenThrow(new NotFoundException(Notification.empty()));

        OrderSummaryAdapter adapter = new OrderSummaryAdapter(facade);
        assertThat(adapter.find(UUID.randomUUID())).isEmpty();
    }
}
