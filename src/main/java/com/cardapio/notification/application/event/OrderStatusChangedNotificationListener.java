package com.cardapio.notification.application.event;

import com.cardapio.notification.api.dto.OrderStreamEvent;
import com.cardapio.notification.application.command.EnqueueNotificationCommand;
import com.cardapio.notification.application.usecase.EnqueueNotificationUseCase;
import com.cardapio.notification.domain.model.CustomerContact;
import com.cardapio.notification.domain.model.NotificationChannel;
import com.cardapio.notification.domain.model.NotificationTemplate;
import com.cardapio.notification.domain.port.CustomerContactPort;
import com.cardapio.notification.domain.port.OrderSummaryPort;
import com.cardapio.notification.domain.port.SseBroadcaster;
import com.cardapio.ordering.domain.event.OrderStatusChanged;
import com.cardapio.ordering.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
class OrderStatusChangedNotificationListener {

    private final EnqueueNotificationUseCase enqueue;
    private final SseBroadcaster sse;
    private final CustomerContactPort contacts;
    private final OrderSummaryPort orders;

    @ApplicationModuleListener
    public void on(OrderStatusChanged event) {
        if (event.to() == OrderStatus.CANCELED) {
            // OrderCanceled listener handles this transition.
            return;
        }
        var summary = orders.find(event.orderId().value()).orElse(null);
        if (summary == null) return;

        OrderStreamEvent payload = OrderStreamEvent.statusChanged(
            summary.orderId(), summary.shortRef(),
            event.from() == null ? null : event.from().name(),
            event.to().name());
        sse.broadcastAdmin("order-status-changed", payload);
        sse.broadcastToCustomer(summary.customerId(), "order-status-changed", payload);

        contacts.find(summary.customerId()).ifPresent(c -> {
            Map<String, Object> model = buildModel(c, summary, event.from());
            if (c.email() != null && !c.email().isBlank()) {
                enqueue.execute(new EnqueueNotificationCommand(
                    NotificationChannel.EMAIL, NotificationTemplate.ORDER_STATUS_CHANGED,
                    c.id(), c.email(), model));
            }
            if (c.phoneNumber() != null && !c.phoneNumber().isBlank()) {
                enqueue.execute(new EnqueueNotificationCommand(
                    NotificationChannel.WHATSAPP, NotificationTemplate.ORDER_STATUS_CHANGED,
                    c.id(), c.phoneNumber(), model));
            }
        });
    }

    private static Map<String, Object> buildModel(CustomerContact c, OrderSummaryPort.OrderSummary s, OrderStatus prev) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("customer", Map.of("name", c.name()));
        model.put("order", Map.of(
            "shortRef", s.shortRef(),
            "status", s.status().name(),
            "previousStatus", prev == null ? "" : prev.name(),
            "modality", s.modality().name(),
            "total", s.total().toPlainString()
        ));
        return model;
    }
}
