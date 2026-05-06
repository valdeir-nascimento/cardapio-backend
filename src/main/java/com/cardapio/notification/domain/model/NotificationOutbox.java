package com.cardapio.notification.domain.model;

import com.cardapio.shared.domain.AggregateRoot;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class NotificationOutbox extends AggregateRoot<NotificationOutboxId> {

    public static final int MAX_ATTEMPTS = 5;

    private static final Duration[] BACKOFF = {
        Duration.ofMinutes(1),
        Duration.ofMinutes(5),
        Duration.ofMinutes(15),
        Duration.ofHours(1)
    };

    private final NotificationChannel channel;
    private final NotificationTemplate template;
    private final UUID recipientId;
    private final String payload;
    private NotificationStatus status;
    private int attempts;
    private String lastError;
    private Instant scheduledFor;
    private final Instant createdAt;
    private Instant updatedAt;

    private NotificationOutbox(NotificationOutboxId id, NotificationChannel channel,
                               NotificationTemplate template, UUID recipientId, String payload,
                               NotificationStatus status, int attempts, String lastError,
                               Instant scheduledFor, Instant createdAt, Instant updatedAt) {
        super(id);
        this.channel = Objects.requireNonNull(channel, "channel");
        this.template = Objects.requireNonNull(template, "template");
        this.recipientId = Objects.requireNonNull(recipientId, "recipientId");
        this.payload = Objects.requireNonNull(payload, "payload");
        this.status = Objects.requireNonNull(status, "status");
        this.attempts = attempts;
        this.lastError = lastError;
        this.scheduledFor = Objects.requireNonNull(scheduledFor, "scheduledFor");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static NotificationOutbox enqueue(NotificationChannel channel, NotificationTemplate template,
                                             UUID recipientId, String payload, Clock clock) {
        if (!channel.isPersisted()) {
            throw new IllegalArgumentException("channel " + channel + " is not persisted");
        }
        Instant now = clock.instant();
        return new NotificationOutbox(
            NotificationOutboxId.newId(), channel, template, recipientId, payload,
            NotificationStatus.PENDING, 0, null, now, now, now);
    }

    public static NotificationOutbox rehydrate(NotificationOutboxId id, NotificationChannel channel,
                                               NotificationTemplate template, UUID recipientId, String payload,
                                               NotificationStatus status, int attempts, String lastError,
                                               Instant scheduledFor, Instant createdAt, Instant updatedAt) {
        return new NotificationOutbox(id, channel, template, recipientId, payload, status, attempts,
            lastError, scheduledFor, createdAt, updatedAt);
    }

    public void markSent(Clock clock) {
        if (status.isTerminal()) {
            throw new IllegalStateException("already terminal: " + status);
        }
        this.status = NotificationStatus.SENT;
        this.attempts++;
        this.lastError = null;
        this.updatedAt = clock.instant();
    }

    public void markFailed(String error, Clock clock) {
        if (status.isTerminal()) {
            throw new IllegalStateException("already terminal: " + status);
        }
        this.attempts++;
        this.lastError = error;
        Instant now = clock.instant();
        this.updatedAt = now;
        if (attempts >= MAX_ATTEMPTS) {
            this.status = NotificationStatus.ABANDONED;
        } else {
            this.status = NotificationStatus.FAILED;
            Duration delay = BACKOFF[Math.min(attempts - 1, BACKOFF.length - 1)];
            this.scheduledFor = now.plus(delay);
        }
    }

    public boolean isDue(Instant now) {
        return status == NotificationStatus.PENDING || status == NotificationStatus.FAILED
            ? !scheduledFor.isAfter(now)
            : false;
    }

    public NotificationChannel channel() { return channel; }
    public NotificationTemplate template() { return template; }
    public UUID recipientId() { return recipientId; }
    public String payload() { return payload; }
    public NotificationStatus status() { return status; }
    public int attempts() { return attempts; }
    public String lastError() { return lastError; }
    public Instant scheduledFor() { return scheduledFor; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
