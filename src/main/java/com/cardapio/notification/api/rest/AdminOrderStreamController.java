package com.cardapio.notification.api.rest;

import com.cardapio.notification.infrastructure.sse.SseEmitterRegistry;
import com.cardapio.shared.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Tag(name = "Notification — Admin SSE",
    description = "Stream Server-Sent Events com eventos de pedidos para o painel administrativo.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
class AdminOrderStreamController {

    private final SseEmitterRegistry registry;

    @Operation(summary = "Abre stream SSE de eventos de pedidos (admin)",
        description = """
            Mantém uma conexão HTTP aberta usando `text/event-stream`.
            O servidor publica eventos quando qualquer pedido é criado ou tem status alterado.

            ### Eventos enviados
            - `connected` — emitido logo após a conexão (`data: ok`)
            - `order.placed` — pedido novo
            - `order.status_changed` — mudança de status

            ### Reconexão
            Se a conexão cair, o cliente reconecta automaticamente. Não há replay de eventos
            perdidos — use `GET /api/v1/admin/orders` para reconciliar estado.
            """)
    @ApiResponse(responseCode = "200", description = "Stream aberto.",
        content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
            schema = @Schema(type = "string",
                example = "event: order.placed\\ndata: {\"id\":\"21a98176-44df-49eb-a37c-b8329f1e0123\",\"status\":\"PLACED\"}\\n\\n")))
    @GetMapping(value = "/api/v1/admin/orders/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream() {
        SseEmitter emitter = registry.registerAdmin();
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ignored) {
            // Client disconnected before initial send; cleanup hooks already remove the emitter.
        }
        return emitter;
    }
}
