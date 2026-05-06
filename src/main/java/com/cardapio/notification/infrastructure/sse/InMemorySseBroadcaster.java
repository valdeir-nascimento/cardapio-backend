package com.cardapio.notification.infrastructure.sse;

import com.cardapio.notification.domain.port.SseBroadcaster;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class InMemorySseBroadcaster implements SseBroadcaster {

    private final SseEmitterRegistry registry;

    @Override
    public void broadcastAdmin(String eventName, Object payload) {
        registry.sendToAdmins(eventName, payload);
    }

    @Override
    public void broadcastToCustomer(UUID customerId, String eventName, Object payload) {
        registry.sendToCustomer(customerId, eventName, payload);
    }
}
