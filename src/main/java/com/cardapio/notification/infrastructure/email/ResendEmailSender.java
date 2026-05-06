package com.cardapio.notification.infrastructure.email;

import com.cardapio.notification.domain.exception.NotificationDispatchException;
import com.cardapio.notification.domain.port.EmailSender;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "resend.enabled", havingValue = "true")
@Slf4j
class ResendEmailSender implements EmailSender {

    private final RestClient client;
    private final ResendProperties props;

    ResendEmailSender(@Qualifier("resendRestClient") RestClient client, ResendProperties props) {
        this.client = client;
        this.props = props;
    }

    @Retry(name = "resend")
    @CircuitBreaker(name = "resend")
    @Override
    public void send(String to, String subject, String html, String text) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", props.from());
        body.put("to", new String[]{to});
        body.put("subject", subject);
        body.put("html", html);
        if (text != null) body.put("text", text);

        try {
            client.post()
                .uri("/emails")
                .body(body)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String responseBody = e.getResponseBodyAsString();
            log.warn("resend send failed status={} body={}", status, responseBody);
            throw new NotificationDispatchException(
                status >= 500 ? "RESEND_5XX" : "RESEND_4XX",
                "Resend returned status " + status + ": " + responseBody,
                e);
        } catch (Exception e) {
            throw new NotificationDispatchException("RESEND_TRANSPORT", e.getMessage(), e);
        }
    }
}
