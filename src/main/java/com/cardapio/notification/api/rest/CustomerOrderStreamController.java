package com.cardapio.notification.api.rest;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.notification.domain.port.OrderSummaryPort;
import com.cardapio.notification.infrastructure.sse.SseEmitterRegistry;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Notification — Customer SSE",
    description = "Stream SSE para o cliente acompanhar a evolução do próprio pedido em tempo real.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
class CustomerOrderStreamController {

    private final SseEmitterRegistry registry;
    private final OrderSummaryPort orderSummary;

    @Operation(summary = "Abre stream SSE de status de um pedido específico do cliente",
        description = """
            Conexão `text/event-stream` que recebe `order.status_changed` sempre que
            o pedido informado mudar de status (`PLACED → CONFIRMED → PREPARING → ...`).

            ### Autorização
            Verifica que o pedido pertence ao cliente autenticado. Caso contrário, retorna 403.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stream aberto.",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                schema = @Schema(type = "string",
                    example = "event: order.status_changed\\ndata: {\"id\":\"21a98176-...\",\"status\":\"PREPARING\"}\\n\\n"))),
        @ApiResponse(responseCode = "403", description = "Pedido não pertence ao cliente."),
        @ApiResponse(responseCode = "404", description = "Pedido não existe.")
    })
    @GetMapping(value = "/api/v1/orders/{id}/status/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @Parameter(description = "ID do pedido.", example = "21a98176-44df-49eb-a37c-b8329f1e0123")
        @PathVariable UUID id
    ) {
        var summary = orderSummary.find(id)
            .orElseThrow(() -> new NotFoundException(Notification.empty()));
        if (!summary.customerId().equals(me.subject())) {
            throw new AccessDeniedException("order does not belong to caller");
        }

        SseEmitter emitter = registry.registerCustomer(me.subject());
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ignored) {
            // Client disconnected before initial send.
        }
        return emitter;
    }
}
