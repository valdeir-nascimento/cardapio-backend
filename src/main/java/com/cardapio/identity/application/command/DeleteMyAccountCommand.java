package com.cardapio.identity.application.command;

import com.cardapio.identity.domain.model.CustomerId;

public record DeleteMyAccountCommand(CustomerId customerId) {}
