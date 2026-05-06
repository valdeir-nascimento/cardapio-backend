package com.cardapio.notification.domain.model;

public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    ABANDONED;

    public boolean isTerminal() {
        return this == SENT || this == ABANDONED;
    }
}
