package com.cardapio.payment.domain.port;

public interface WebhookPayloadParser {

    Parsed parse(String rawBody);

    record Parsed(String type, String dataId) {}
}
