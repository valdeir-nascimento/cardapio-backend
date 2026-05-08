package com.cardapio.ordering.api.rest;

import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.ordering.api.dto.OrderResponse;
import com.cardapio.ordering.api.dto.OrderSummaryResponse;
import com.cardapio.ordering.api.dto.PlaceOrderRequest;
import com.cardapio.ordering.api.dto.PlaceOrderResponse;
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
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(name = "Ordering — Orders (Customer)", description = "Checkout e consulta de pedidos do próprio cliente.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface OrderApi {

    @Operation(summary = "Cria um pedido a partir do carrinho atual",
        description = """
            Fecha o carrinho do cliente como pedido e zera o carrinho.
            Sujeito a rate limit de **60 requisições/minuto** por IP.

            ### Idempotência

            O header `Idempotency-Key` é **obrigatório**. Se o cliente fizer
            duas chamadas com a mesma chave (ex: por retry), a segunda
            retorna o mesmo pedido sem duplicar.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pedido criado.",
            content = @Content(schema = @Schema(implementation = PlaceOrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Carrinho vazio, endereço faltando para DELIVERY, etc.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"CART_EMPTY","message":"Carrinho está vazio."}
                    """))),
        @ApiResponse(responseCode = "409", description = "Algum item ficou indisponível durante o checkout.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "429", description = "Rate limit excedido.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ResponseEntity<PlaceOrderResponse> place(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @Parameter(description = "Chave única para idempotência (UUID v4 recomendado).",
            example = "9d8e7f6c-1234-5678-9abc-def012345678", required = true)
        @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
        @Valid @RequestBody PlaceOrderRequest body);

    @Operation(summary = "Detalhes de um pedido do cliente logado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pedido retornado.",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "404", description = "Pedido não pertence a este cliente ou não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    OrderResponse get(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @Parameter(description = "ID do pedido.", example = "21a98176-44df-49eb-a37c-b8329f1e0123")
        @PathVariable UUID id);

    @Operation(summary = "Histórico de pedidos do cliente logado",
        description = "Retorna sumários ordenados do mais recente para o mais antigo.")
    @ApiResponse(responseCode = "200", description = "Lista retornada.")
    List<OrderSummaryResponse> mine(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @Parameter(description = "Quantos resultados por página.", example = "20")
        @RequestParam(defaultValue = "20") int limit,
        @Parameter(description = "Quantos resultados pular (paginação).", example = "0")
        @RequestParam(defaultValue = "0") int offset);
}
