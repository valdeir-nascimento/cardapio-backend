package com.cardapio.notification.domain.model;

import java.util.Objects;
import java.util.UUID;

public record NotificationOutboxId(UUID value) {
    public NotificationOutboxId { Objects.requireNonNull(value, "value"); }
    public static NotificationOutboxId newId() { return new NotificationOutboxId(UUID.randomUUID()); }
    public static NotificationOutboxId of(UUID value) { return new NotificationOutboxId(value); }
    public static NotificationOutboxId of(String value) { return new NotificationOutboxId(UUID.fromString(value)); }
}
