package com.cardapio.ordering.api.rest;

import com.cardapio.ordering.api.dto.AdvanceStatusRequest;
import com.cardapio.ordering.api.dto.OrderResponse;
import com.cardapio.ordering.api.dto.OrderSummaryResponse;
import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.shared.openapi.ApiErrorResponse;
import com.cardapio.shared.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Ordering — Orders (Admin)", description = "Listagem, consulta e mudança de status de pedidos pelo painel.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface OrderAdminApi {

    @Operation(summary = "Lista pedidos com filtros",
        description = """
            Filtros são opcionais. Sem filtros retorna os mais recentes.
            Combine `from` e `to` para janelas temporais (ex: pedidos de hoje).
            """)
    @ApiResponse(responseCode = "200", description = "Lista retornada.")
    List<OrderSummaryResponse> list(
        @Parameter(description = "Filtra por status atual.", example = "PREPARING")
        @RequestParam(required = false) OrderStatus status,
        @Parameter(description = "Limite inferior de `placedAt` (UTC ISO-8601).",
            example = "2026-05-07T00:00:00Z")
        @RequestParam(required = false) Instant from,
        @Parameter(description = "Limite superior de `placedAt`.",
            example = "2026-05-08T00:00:00Z")
        @RequestParam(required = false) Instant to,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset);

    @Operation(summary = "Detalhes completos de um pedido (qualquer cliente)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido retornado.",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "404", description = "Pedido não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    OrderResponse get(@PathVariable UUID id);

    @Operation(summary = "Avança o pedido para o próximo status",
        description = """
            Transições válidas (cada → admite só o próximo):
            `PLACED → CONFIRMED → PREPARING → READY → DELIVERING → DELIVERED`.

            Atalhos: pedidos PICKUP pulam DELIVERING. Cancelamento usa endpoint separado.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Status atualizado."),
        @ApiResponse(responseCode = "409", description = "Transição inválida (ex: tentar voltar status).",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"ORDER_INVALID_TRANSITION","message":"Não é possível voltar status do pedido."}
                    """)))
    })
    ResponseEntity<Void> advance(@PathVariable UUID id, @Valid @RequestBody AdvanceStatusRequest body);

    @Operation(summary = "Cancela um pedido (somente OWNER ou MANAGER)",
        description = "Não permitido após o pedido ser DELIVERED. Inicia processo de reembolso se já houve pagamento.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Pedido cancelado."),
        @ApiResponse(responseCode = "409", description = "Pedido em estado que não permite cancelamento.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ResponseEntity<Void> cancel(@PathVariable UUID id);
}
