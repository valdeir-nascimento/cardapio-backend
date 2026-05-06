package com.cardapio.notification.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_outbox")
public class NotificationOutboxJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(nullable = false, length = 20) private String channel;
    @Column(nullable = false, length = 50) private String template;
    @Column(name = "recipient_id", nullable = false) private UUID recipientId;
    @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    @Column(nullable = false, length = 20) private String status;
    @Column(nullable = false) private int attempts;
    @Column(name = "last_error", columnDefinition = "TEXT") private String lastError;
    @Column(name = "scheduled_for", nullable = false) private Instant scheduledFor;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected NotificationOutboxJpaEntity() {}

    public NotificationOutboxJpaEntity(UUID id, String channel, String template, UUID recipientId,
                                       String payload, String status, int attempts, String lastError,
                                       Instant scheduledFor, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.channel = channel;
        this.template = template;
        this.recipientId = recipientId;
        this.payload = payload;
        this.status = status;
        this.attempts = attempts;
        this.lastError = lastError;
        this.scheduledFor = scheduledFor;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getChannel() { return channel; }
    public String getTemplate() { return template; }
    public UUID getRecipientId() { return recipientId; }
    public String getPayload() { return payload; }
    public String getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public Instant getScheduledFor() { return scheduledFor; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(String status) { this.status = status; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public void setScheduledFor(Instant scheduledFor) { this.scheduledFor = scheduledFor; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
