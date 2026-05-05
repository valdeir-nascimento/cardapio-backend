package com.cardapio.api.error;

import com.cardapio.shared.domain.Notification;

public class NotificationException extends RuntimeException {
    private final Notification notification;

    public NotificationException(Notification notification) {
        super("notification has errors");
        this.notification = notification;
    }

    public Notification notification() {
        return notification;
    }
}
