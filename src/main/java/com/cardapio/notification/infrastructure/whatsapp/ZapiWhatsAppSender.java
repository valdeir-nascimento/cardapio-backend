package com.cardapio.notification.infrastructure.whatsapp;

import com.cardapio.notification.domain.exception.NotificationDispatchException;
import com.cardapio.notification.domain.port.WhatsAppSender;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "zapi.enabled", havingValue = "true")
@Slf4j
class ZapiWhatsAppSender implements WhatsAppSender {

    private final RestClient client;
    private final ZapiProperties props;

    ZapiWhatsAppSender(@Qualifier("zapiRestClient") RestClient client, ZapiProperties props) {
        this.client = client;
        this.props = props;
    }

    @Retry(name = "zapi")
    @CircuitBreaker(name = "zapi")
    @Override
    public void sendText(String phoneE164, String body) {
        String phone = normalize(phoneE164);
        Map<String, Object> payload = Map.of(
            "phone", phone,
            "message", body
        );
        String uri = "/instances/" + props.instanceId() + "/token/" + props.token() + "/send-text";
        try {
            client.post()
                .uri(uri)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String responseBody = e.getResponseBodyAsString();
            log.warn("zapi send failed status={} body={}", status, responseBody);
            throw new NotificationDispatchException(
                status >= 500 ? "ZAPI_5XX" : "ZAPI_4XX",
                "Z-API returned status " + status + ": " + responseBody,
                e);
        } catch (Exception e) {
            throw new NotificationDispatchException("ZAPI_TRANSPORT", e.getMessage(), e);
        }
    }

    /**
     * Z-API expects digits only (e.g. {@code 5511999998888}). Strips '+',
     * spaces, dashes, and parentheses from the supplied E.164 number.
     */
    static String normalize(String phoneE164) {
        if (phoneE164 == null || phoneE164.isBlank()) {
            throw new IllegalArgumentException("phone is blank");
        }
        StringBuilder sb = new StringBuilder(phoneE164.length());
        for (char c : phoneE164.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
        }
        if (sb.isEmpty()) {
            throw new IllegalArgumentException("phone has no digits: " + phoneE164);
        }
        return sb.toString();
    }
}
