package com.cardapio.payment.application.command;

import com.cardapio.payment.domain.model.PaymentMethod;

import java.util.UUID;

public record InitiatePaymentCommand(
    UUID orderId,
    UUID customerId,
    PaymentMethod method,
    String cardToken
) {}
