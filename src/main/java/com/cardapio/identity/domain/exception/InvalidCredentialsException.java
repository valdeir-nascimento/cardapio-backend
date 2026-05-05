package com.cardapio.identity.domain.exception;

import com.cardapio.shared.domain.DomainException;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "credenciais inválidas");
    }
}
