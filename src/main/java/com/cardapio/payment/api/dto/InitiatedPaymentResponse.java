package com.cardapio.payment.api.dto;

import com.cardapio.payment.application.dto.InitiatedPaymentView;
import com.cardapio.payment.domain.model.PaymentMethod;
import com.cardapio.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiatedPaymentResponse(
    UUID id,
    PaymentMethod method,
    PaymentStatus status,
    BigDecimal amount,
    String currency,
    String qrCode,
    String qrCodeBase64,
    String cardBrand,
    String cardLast4
) {
    public static InitiatedPaymentResponse from(InitiatedPaymentView v) {
        return new InitiatedPaymentResponse(
            v.id(), v.method(), v.status(),
            v.amount(), v.currency(),
            v.qrCode(), v.qrCodeBase64(),
            v.cardBrand(), v.cardLast4()
        );
    }
}
