package com.cardapio.payment.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WebhookEventLog(
    UUID id,
    String payloadHash,
    String rawPayload,
    Instant receivedAt
) {
    public static final int MAX_PAYLOAD_LENGTH = 8000;

    public WebhookEventLog {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(payloadHash, "payloadHash");
        Objects.requireNonNull(receivedAt, "receivedAt");
        if (rawPayload != null && rawPayload.length() > MAX_PAYLOAD_LENGTH) {
            rawPayload = rawPayload.substring(0, MAX_PAYLOAD_LENGTH);
        }
    }
}
