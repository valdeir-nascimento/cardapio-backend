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
import com.cardapio.ordering.domain.event.OrderPlaced;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
class OrderPlacedNotificationListener {

    private final EnqueueNotificationUseCase enqueue;
    private final SseBroadcaster sse;
    private final CustomerContactPort contacts;
    private final OrderSummaryPort orders;

    @ApplicationModuleListener
    public void on(OrderPlaced event) {
        var summary = orders.find(event.orderId().value()).orElse(null);
        if (summary == null) {
            log.warn("OrderPlaced received but order {} not found", event.orderId().value());
            return;
        }

        sse.broadcastAdmin("new-order", OrderStreamEvent.placed(
            summary.orderId(), summary.shortRef(),
            summary.modality().name(), summary.total()));

        contacts.find(event.customerId()).ifPresentOrElse(c -> {
            Map<String, Object> model = buildModel(c, summary);
            if (c.email() != null && !c.email().isBlank()) {
                enqueue.execute(new EnqueueNotificationCommand(
                    NotificationChannel.EMAIL, NotificationTemplate.ORDER_RECEIVED,
                    c.id(), c.email(), model));
            }
            if (c.phoneNumber() != null && !c.phoneNumber().isBlank()) {
                enqueue.execute(new EnqueueNotificationCommand(
                    NotificationChannel.WHATSAPP, NotificationTemplate.ORDER_RECEIVED,
                    c.id(), c.phoneNumber(), model));
            }
        }, () -> log.warn("OrderPlaced for unknown customer {}", event.customerId()));
    }

    private static Map<String, Object> buildModel(CustomerContact c, OrderSummaryPort.OrderSummary s) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("customer", Map.of("name", c.name()));
        model.put("order", Map.of(
            "shortRef", s.shortRef(),
            "modality", s.modality().name(),
            "status", s.status().name(),
            "total", s.total().toPlainString()
        ));
        return model;
    }
}
