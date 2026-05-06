package com.cardapio.notification.domain.model;

public enum NotificationTemplate {
    ORDER_RECEIVED,
    ORDER_STATUS_CHANGED,
    ORDER_CANCELED,
    PAYMENT_APPROVED,
    PAYMENT_REJECTED;

    public String key() {
        return name().toLowerCase().replace('_', '-');
    }
}
