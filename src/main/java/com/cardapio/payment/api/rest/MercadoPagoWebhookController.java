package com.cardapio.payment.api.rest;

import com.cardapio.payment.application.PaymentFacade;
import com.cardapio.payment.application.command.HandleWebhookCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payment — Webhook (Mercado Pago)",
    description = "Endpoint chamado pelo Mercado Pago quando o status do pagamento muda.")
@SecurityRequirements
public class MercadoPagoWebhookController {

    private final PaymentFacade payments;

    @Operation(summary = "Recebe notificações do Mercado Pago",
        description = """
            Endpoint chamado **pelo Mercado Pago** (não pelo cliente) sempre que
            há mudança em um pagamento.

            ### Validação de assinatura
            O backend valida a assinatura HMAC dos headers `x-signature` e
            `x-request-id` contra o `MP_WEBHOOK_SECRET`. Notificações com
            assinatura inválida são descartadas silenciosamente.

            ### Idempotência
            O Mercado Pago pode reenviar a mesma notificação várias vezes em
            caso de falha. O backend é idempotente: processar a mesma `data.id`
            duas vezes é seguro.

            ### Estratégia de resposta
            Sempre responde 200 (mesmo em erro) para evitar retries em loop pelo MP.
            """)
    @ApiResponse(
        responseCode = "200",
        description = "Notificação recebida (mesmo se ignorada por assinatura inválida).",
        content = @Content(
            schema = @Schema(
                description = "Payload simplificado conforme MP. Headers `x-signature` e `x-request-id` carregam a assinatura.",
                example = """
                    {"action":"payment.updated","api_version":"v1","data":{"id":"123456"},"date_created":"2026-05-07T20:30:00Z","id":123456,"live_mode":false,"type":"payment","user_id":436667006}
                    """),
            examples = @ExampleObject(value = """
                {"action":"payment.updated","api_version":"v1","data":{"id":"123456"},"date_created":"2026-05-07T20:30:00Z","id":123456,"live_mode":false,"type":"payment","user_id":436667006}
                """)
        ))
    @PostMapping("/mercado-pago")
    public ResponseEntity<Void> mercadoPago(
        @Parameter(description = "Payload JSON cru do Mercado Pago. Lido como string para validação HMAC byte-a-byte.")
        @RequestBody String rawBody,
        @Parameter(hidden = true) @RequestHeader Map<String, String> headers
    ) {
        try {
            payments.handleWebhook(new HandleWebhookCommand(rawBody, headers));
        } catch (Exception e) {
            log.warn("webhook handler error (responding 200 to prevent MP retry storms): {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
