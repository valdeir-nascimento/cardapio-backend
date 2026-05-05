package com.cardapio.identity.application.command;

public record LoginCommand(String email, String rawPassword) {}
