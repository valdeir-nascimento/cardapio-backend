package com.cardapio.payment.application.command;

import com.cardapio.payment.domain.model.PaymentTransactionId;

public record RefundPaymentCommand(PaymentTransactionId paymentId) {}
