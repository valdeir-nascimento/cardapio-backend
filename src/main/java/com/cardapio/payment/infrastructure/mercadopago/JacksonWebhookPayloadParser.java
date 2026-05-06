package com.cardapio.payment.infrastructure.mercadopago;

import com.cardapio.payment.domain.port.WebhookPayloadParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JacksonWebhookPayloadParser implements WebhookPayloadParser {

    private final ObjectMapper json = new ObjectMapper();

    @Override
    public Parsed parse(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) return new Parsed(null, null);
        try {
            JsonNode root = json.readTree(rawBody);
            String type = textOrNull(root.get("type"));
            JsonNode data = root.get("data");
            String dataId = data != null ? textOrNull(data.get("id")) : null;
            return new Parsed(type, dataId);
        } catch (Exception e) {
            return new Parsed(null, null);
        }
    }

    private static String textOrNull(JsonNode node) {
        return node != null && !node.isNull() ? node.asText() : null;
    }
}
