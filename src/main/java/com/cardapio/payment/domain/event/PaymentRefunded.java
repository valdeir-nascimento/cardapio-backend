package com.cardapio.payment.domain.event;

import com.cardapio.payment.domain.model.PaymentTransaction;
import com.cardapio.payment.domain.model.PaymentTransactionId;
import com.cardapio.shared.domain.DomainEvent;
import com.cardapio.shared.domain.Money;

import java.time.Instant;
import java.util.UUID;

public record PaymentRefunded(
    UUID id,
    Instant occurredOn,
    PaymentTransactionId paymentId,
    UUID orderId,
    UUID customerId,
    Money amount
) implements DomainEvent {

    public static PaymentRefunded of(PaymentTransaction tx, Instant occurredOn) {
        return new PaymentRefunded(UUID.randomUUID(), occurredOn,
            tx.id(), tx.orderId(), tx.customerId(), tx.amount());
    }
}
