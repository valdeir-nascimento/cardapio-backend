package com.cardapio.payment.api.rest;

import com.cardapio.payment.application.PaymentFacade;
import com.cardapio.payment.application.command.HandleWebhookCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class MercadoPagoWebhookController {

    private final PaymentFacade payments;

    @PostMapping("/mercado-pago")
    public ResponseEntity<Void> mercadoPago(
        @RequestBody String rawBody,
        @RequestHeader Map<String, String> headers
    ) {
        try {
            payments.handleWebhook(new HandleWebhookCommand(rawBody, headers));
        } catch (Exception e) {
            log.warn("webhook handler error (responding 200 to prevent MP retry storms): {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
