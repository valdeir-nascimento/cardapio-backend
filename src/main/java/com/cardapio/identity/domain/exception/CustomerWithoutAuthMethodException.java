package com.cardapio.identity.domain.exception;

public class CustomerWithoutAuthMethodException extends RuntimeException {
    public CustomerWithoutAuthMethodException() {
        super("customer must have at least one auth method (password or social identity)");
    }
}
