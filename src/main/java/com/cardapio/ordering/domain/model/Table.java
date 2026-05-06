package com.cardapio.ordering.domain.model;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Table {

    private final TableId id;
    private final int number;
    private final UUID qrToken;
    private String qrImageKey;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Table(TableId id, int number, UUID qrToken, String qrImageKey, boolean active,
                  Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        if (number <= 0) {
            throw new IllegalArgumentException("table number must be positive");
        }
        this.number = number;
        this.qrToken = Objects.requireNonNull(qrToken, "qrToken");
        this.qrImageKey = qrImageKey;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Table create(int number, Clock clock) {
        Objects.requireNonNull(clock, "clock");
        Instant now = clock.instant();
        return new Table(TableId.newId(), number, UUID.randomUUID(), null, true, now, now);
    }

    public static Table rehydrate(TableId id, int number, UUID qrToken, String qrImageKey,
                                  boolean active, Instant createdAt, Instant updatedAt) {
        return new Table(id, number, qrToken, qrImageKey, active, createdAt, updatedAt);
    }

    public void attachQrImageKey(String key, Clock clock) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("qrImageKey must not be blank");
        }
        this.qrImageKey = key;
        this.updatedAt = clock.instant();
    }

    public void deactivate(Clock clock) {
        this.active = false;
        this.updatedAt = clock.instant();
    }

    public void activate(Clock clock) {
        this.active = true;
        this.updatedAt = clock.instant();
    }

    public TableId id() { return id; }
    public int number() { return number; }
    public UUID qrToken() { return qrToken; }
    public String qrImageKey() { return qrImageKey; }
    public boolean isActive() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
