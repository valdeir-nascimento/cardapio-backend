package com.cardapio.payment.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_webhook_events")
public class PaymentWebhookEventJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "payment_tx_id", nullable = false) private UUID paymentTxId;
    @Column(name = "payload_hash", nullable = false, length = 64) private String payloadHash;
    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT") private String rawPayload;
    @Column(name = "received_at", nullable = false) private Instant receivedAt;
    @Column(nullable = false) private int position;

    protected PaymentWebhookEventJpaEntity() {}

    public PaymentWebhookEventJpaEntity(UUID id, UUID paymentTxId, String payloadHash,
                                        String rawPayload, Instant receivedAt, int position) {
        this.id = id; this.paymentTxId = paymentTxId; this.payloadHash = payloadHash;
        this.rawPayload = rawPayload; this.receivedAt = receivedAt; this.position = position;
    }

    public UUID getId() { return id; }
    public UUID getPaymentTxId() { return paymentTxId; }
    public String getPayloadHash() { return payloadHash; }
    public String getRawPayload() { return rawPayload; }
    public Instant getReceivedAt() { return receivedAt; }
    public int getPosition() { return position; }
}
