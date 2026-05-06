package com.cardapio.notification.application.event;

import com.cardapio.notification.api.dto.PaymentStreamEvent;
import com.cardapio.notification.domain.port.SseBroadcaster;
import com.cardapio.payment.domain.event.PaymentApproved;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PaymentApprovedNotificationListener {

    private final SseBroadcaster sse;

    /**
     * Customer-facing email/WhatsApp is intentionally not enqueued here:
     * payment approval triggers ordering's PaymentApprovedListener
     * (Phase 3) which advances the order to CONFIRMED, fires
     * OrderStatusChanged, and the OrderStatusChanged listener delivers
     * the customer-facing notification — sending one here too would
     * duplicate the message.
     */
    @ApplicationModuleListener
    public void on(PaymentApproved event) {
        sse.broadcastAdmin("payment-approved", new PaymentStreamEvent(
            event.paymentId().value(),
            event.orderId(),
            event.method().name(),
            "APPROVED",
            event.amount().amount()
        ));
    }
}
