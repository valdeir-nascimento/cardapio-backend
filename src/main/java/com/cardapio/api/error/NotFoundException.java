package com.cardapio.api.error;

import com.cardapio.shared.domain.Notification;

public class NotFoundException extends NotificationException {
    public NotFoundException(Notification notification) {
        super(notification);
    }
}
