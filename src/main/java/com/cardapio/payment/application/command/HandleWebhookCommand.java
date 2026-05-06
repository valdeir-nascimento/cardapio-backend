package com.cardapio.payment.application.command;

import java.util.Map;

public record HandleWebhookCommand(String rawBody, Map<String, String> headers) {}
