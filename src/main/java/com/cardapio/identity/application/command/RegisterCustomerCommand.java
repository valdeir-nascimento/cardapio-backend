package com.cardapio.identity.application.command;

public record RegisterCustomerCommand(
    String name,
    String email,
    String phoneNumber,
    String rawPassword
) {}
