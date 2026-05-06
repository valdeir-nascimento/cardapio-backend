package com.cardapio.payment.domain.exception;

import com.cardapio.payment.domain.model.PaymentTransactionId;
import com.cardapio.shared.domain.DomainException;

public class PaymentTransactionNotFoundException extends DomainException {
    public PaymentTransactionNotFoundException(PaymentTransactionId id) {
        super("PAYMENT_NOT_FOUND", "payment transaction not found: " + id.value());
    }
}
