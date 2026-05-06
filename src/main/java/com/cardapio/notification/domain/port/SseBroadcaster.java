package com.cardapio.notification.domain.port;

import java.util.UUID;

public interface SseBroadcaster {

    void broadcastAdmin(String eventName, Object payload);

    void broadcastToCustomer(UUID customerId, String eventName, Object payload);
}
