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
import com.cardapio.ordering.domain.event.OrderCanceled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
class OrderCanceledNotificationListener {

    private final EnqueueNotificationUseCase enqueue;
    private final SseBroadcaster sse;
    private final CustomerContactPort contacts;
    private final OrderSummaryPort orders;

    @ApplicationModuleListener
    public void on(OrderCanceled event) {
        var summary = orders.find(event.orderId().value()).orElse(null);
        String shortRef = summary == null ? "?" : summary.shortRef();

        OrderStreamEvent payload = OrderStreamEvent.canceled(event.orderId().value(), shortRef);
        sse.broadcastAdmin("order-canceled", payload);
        sse.broadcastToCustomer(event.customerId(), "order-canceled", payload);

        contacts.find(event.customerId()).ifPresent(c -> {
            Map<String, Object> model = buildModel(c, shortRef);
            // WhatsApp only — keeps inbox-noise low for cancellation
            if (c.phoneNumber() != null && !c.phoneNumber().isBlank()) {
                enqueue.execute(new EnqueueNotificationCommand(
                    NotificationChannel.WHATSAPP, NotificationTemplate.ORDER_CANCELED,
                    c.id(), c.phoneNumber(), model));
            }
        });
    }

    private static Map<String, Object> buildModel(CustomerContact c, String shortRef) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("customer", Map.of("name", c.name()));
        model.put("order", Map.of("shortRef", shortRef));
        return model;
    }
}
