package com.cardapio.payment.api.dto;

import com.cardapio.payment.application.command.InitiatePaymentCommand;
import com.cardapio.payment.domain.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InitiatePaymentRequest(
    @NotNull PaymentMethod method,
    String cardToken
) {
    public InitiatePaymentCommand toCommand(UUID orderId, UUID customerId) {
        return new InitiatePaymentCommand(orderId, customerId, method, cardToken);
    }
}
