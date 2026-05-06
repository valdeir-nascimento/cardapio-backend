package com.cardapio.payment.domain.model;

import java.util.Objects;
import java.util.UUID;

public record PaymentTransactionId(UUID value) {
    public PaymentTransactionId { Objects.requireNonNull(value, "value"); }
    public static PaymentTransactionId newId() { return new PaymentTransactionId(UUID.randomUUID()); }
    public static PaymentTransactionId of(UUID value) { return new PaymentTransactionId(value); }
    public static PaymentTransactionId of(String value) { return new PaymentTransactionId(UUID.fromString(value)); }
}
