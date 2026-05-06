package com.cardapio.payment.domain.model;

import java.util.Objects;

public record CardCharge(
    String gatewayTxId,
    PaymentStatus status,
    String last4,
    String brand,
    String authorizationCode
) {
    public CardCharge {
        Objects.requireNonNull(gatewayTxId, "gatewayTxId");
        Objects.requireNonNull(status, "status");
        if (status != PaymentStatus.APPROVED && status != PaymentStatus.REJECTED) {
            throw new IllegalArgumentException("card charge must complete with APPROVED or REJECTED, was " + status);
        }
    }
}
