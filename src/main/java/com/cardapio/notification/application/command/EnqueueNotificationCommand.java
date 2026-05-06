package com.cardapio.notification.application.command;

import com.cardapio.notification.domain.model.NotificationChannel;
import com.cardapio.notification.domain.model.NotificationTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record EnqueueNotificationCommand(
    NotificationChannel channel,
    NotificationTemplate template,
    UUID recipientId,
    String to,
    Map<String, Object> model
) {
    public EnqueueNotificationCommand {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(recipientId, "recipientId");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(model, "model");
        if (!channel.isPersisted()) {
            throw new IllegalArgumentException("channel " + channel + " is not persisted; use SseBroadcaster directly");
        }
    }
}
