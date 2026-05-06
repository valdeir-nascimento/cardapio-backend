package com.cardapio.payment.domain.event;

import com.cardapio.payment.domain.model.PaymentMethod;
import com.cardapio.payment.domain.model.PaymentTransaction;
import com.cardapio.payment.domain.model.PaymentTransactionId;
import com.cardapio.shared.domain.DomainEvent;
import com.cardapio.shared.domain.Money;

import java.time.Instant;
import java.util.UUID;

public record PaymentRejected(
    UUID id,
    Instant occurredOn,
    PaymentTransactionId paymentId,
    UUID orderId,
    UUID customerId,
    Money amount,
    PaymentMethod method
) implements DomainEvent {

    public static PaymentRejected of(PaymentTransaction tx, Instant occurredOn) {
        return new PaymentRejected(UUID.randomUUID(), occurredOn,
            tx.id(), tx.orderId(), tx.customerId(), tx.amount(), tx.method());
    }
}
