package com.cardapio.notification.domain.model;

public enum NotificationChannel {
    EMAIL,
    WHATSAPP,
    SSE_ADMIN,
    SSE_CUSTOMER;

    public boolean isPersisted() {
        return this == EMAIL || this == WHATSAPP;
    }
}
