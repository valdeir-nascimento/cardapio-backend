package com.cardapio.payment.domain.port;

import java.time.Instant;

public interface ProcessedWebhookStore {

    /** Returns false if the payload hash was already registered (already processed). */
    boolean registerIfAbsent(String payloadHash, Instant receivedAt);
}
