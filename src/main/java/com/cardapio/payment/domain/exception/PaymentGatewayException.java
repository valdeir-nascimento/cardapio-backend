package com.cardapio.payment.domain.exception;

import com.cardapio.shared.domain.DomainException;

public class PaymentGatewayException extends DomainException {
    public PaymentGatewayException(String code, String message) {
        super(code, message);
    }

    public PaymentGatewayException(String code, String message, Throwable cause) {
        super(code, message);
        initCause(cause);
    }
}
