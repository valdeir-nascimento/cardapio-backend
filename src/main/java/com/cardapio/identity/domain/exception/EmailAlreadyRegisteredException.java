package com.cardapio.identity.domain.exception;

import com.cardapio.shared.domain.DomainException;
import com.cardapio.shared.domain.Email;

public class EmailAlreadyRegisteredException extends DomainException {
    public EmailAlreadyRegisteredException(Email email) {
        super("EMAIL_ALREADY_REGISTERED", "email already registered: " + email.value());
    }
}
