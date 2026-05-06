package com.cardapio.payment.domain.model;

public enum PaymentStatus {
    PENDING, APPROVED, REJECTED, REFUNDED, FAILED;

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == REFUNDED || this == FAILED;
    }
}
