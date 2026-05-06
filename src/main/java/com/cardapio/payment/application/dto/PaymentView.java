package com.cardapio.payment.application.dto;

import com.cardapio.payment.domain.model.PaymentMethod;
import com.cardapio.payment.domain.model.PaymentStatus;
import com.cardapio.payment.domain.model.PaymentTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentView(
    UUID id,
    UUID orderId,
    UUID customerId,
    PaymentMethod method,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    String qrCode,
    String qrCodeBase64,
    String cardBrand,
    String cardLast4,
    String failureReason,
    Instant createdAt,
    Instant updatedAt
) {
    public static PaymentView from(PaymentTransaction tx) {
        return new PaymentView(
            tx.id().value(), tx.orderId(), tx.customerId(),
            tx.method(), tx.status(),
            tx.amount().amount(), tx.amount().currency().getCurrencyCode(),
            tx.qrCode(), tx.qrCodeBase64(),
            tx.cardBrand(), tx.cardLast4(), tx.failureReason(),
            tx.createdAt(), tx.updatedAt()
        );
    }
}
