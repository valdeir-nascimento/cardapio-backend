package com.cardapio.notification.application.event;

import com.cardapio.notification.api.dto.PaymentStreamEvent;
import com.cardapio.notification.application.command.EnqueueNotificationCommand;
import com.cardapio.notification.application.usecase.EnqueueNotificationUseCase;
import com.cardapio.notification.domain.model.CustomerContact;
import com.cardapio.notification.domain.model.NotificationChannel;
import com.cardapio.notification.domain.model.NotificationTemplate;
import com.cardapio.notification.domain.port.CustomerContactPort;
import com.cardapio.notification.domain.port.OrderSummaryPort;
import com.cardapio.notification.domain.port.SseBroadcaster;
import com.cardapio.payment.domain.event.PaymentRejected;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
class PaymentRejectedNotificationListener {

    private final EnqueueNotificationUseCase enqueue;
    private final SseBroadcaster sse;
    private final CustomerContactPort contacts;
    private final OrderSummaryPort orders;

    @ApplicationModuleListener
    public void on(PaymentRejected event) {
        var summary = orders.find(event.orderId()).orElse(null);
        String shortRef = summary == null ? "?" : summary.shortRef();

        sse.broadcastAdmin("payment-rejected", new PaymentStreamEvent(
            event.paymentId().value(), event.orderId(),
            event.method().name(), "REJECTED", event.amount().amount()));

        contacts.find(event.customerId()).ifPresent(c -> {
            Map<String, Object> model = buildModel(c, shortRef);
            if (c.email() != null && !c.email().isBlank()) {
                enqueue.execute(new EnqueueNotificationCommand(
                    NotificationChannel.EMAIL, NotificationTemplate.PAYMENT_REJECTED,
                    c.id(), c.email(), model));
            }
            if (c.phoneNumber() != null && !c.phoneNumber().isBlank()) {
                enqueue.execute(new EnqueueNotificationCommand(
                    NotificationChannel.WHATSAPP, NotificationTemplate.PAYMENT_REJECTED,
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
