package com.cardapio.payment.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "processed_webhooks")
public class ProcessedWebhookJpaEntity {

    @Id
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected ProcessedWebhookJpaEntity() {}

    public ProcessedWebhookJpaEntity(String payloadHash, Instant receivedAt) {
        this.payloadHash = payloadHash;
        this.receivedAt = receivedAt;
    }

    public String getPayloadHash() { return payloadHash; }
    public Instant getReceivedAt() { return receivedAt; }
}
