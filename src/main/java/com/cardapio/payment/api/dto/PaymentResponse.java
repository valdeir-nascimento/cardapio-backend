package com.cardapio.payment.api.dto;

import com.cardapio.payment.application.dto.PaymentView;
import com.cardapio.payment.domain.model.PaymentMethod;
import com.cardapio.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID orderId,
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
    public static PaymentResponse from(PaymentView v) {
        return new PaymentResponse(
            v.id(), v.orderId(), v.method(), v.status(),
            v.amount(), v.currency(),
            v.qrCode(), v.qrCodeBase64(),
            v.cardBrand(), v.cardLast4(), v.failureReason(),
            v.createdAt(), v.updatedAt()
        );
    }
}
