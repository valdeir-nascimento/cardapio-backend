package com.cardapio.api.error;

import com.cardapio.shared.domain.Notification;

public class UnauthorizedException extends NotificationException {
    public UnauthorizedException(Notification notification) {
        super(notification);
    }
}
