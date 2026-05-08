package com.cardapio.ordering.api.rest;

import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.ordering.api.dto.AddCartItemRequest;
import com.cardapio.ordering.api.dto.ApplyCouponRequest;
import com.cardapio.ordering.api.dto.CartResponse;
import com.cardapio.ordering.api.dto.UpdateCartItemRequest;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

import static com.cardapio.ordering.api.rest.CartController.CartItemCreatedResponse;

@Tag(name = "Ordering — Cart", description = "Carrinho do cliente. Cada cliente tem no máximo um carrinho aberto por vez.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface CartApi {

    @Operation(summary = "Retorna o carrinho atual do cliente",
        description = "Cria automaticamente um carrinho vazio se ainda não existir.")
    @ApiResponse(responseCode = "200", description = "Carrinho retornado.",
        content = @Content(schema = @Schema(implementation = CartResponse.class)))
    CartResponse get(@Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me);

    @Operation(summary = "Adiciona um item ao carrinho",
        description = """
            Calcula preço final com base em variação + adicionais + meia-meia.
            Retorna 201 com o `id` do item para uso em PUT/DELETE.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Item adicionado.",
            content = @Content(schema = @Schema(implementation = CartItemCreatedResponse.class))),
        @ApiResponse(responseCode = "404", description = "Produto, variação ou adicional não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Produto indisponível ou seleção inválida (ex: meia-meia em produto que não permite).",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ResponseEntity<CartItemCreatedResponse> add(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @Valid @RequestBody AddCartItemRequest body);

    @Operation(summary = "Edita quantidade e observação de um item no carrinho")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Item atualizado."),
        @ApiResponse(responseCode = "404", description = "Item não pertence ao carrinho deste cliente.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ResponseEntity<Void> update(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @Parameter(description = "ID do item dentro do carrinho.", example = "5fa6f72a-3b8e-4922-a6cb-882c0a32b2e6")
        @PathVariable UUID itemId,
        @Valid @RequestBody UpdateCartItemRequest body);

    @Operation(summary = "Remove um item do carrinho")
    @ApiResponse(responseCode = "204", description = "Item removido (ou já não existia).")
    void remove(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @PathVariable UUID itemId);

    @Operation(summary = "Aplica um cupom de desconto ao carrinho",
        description = "Substitui qualquer cupom já aplicado. Recalcula `discount` e `discountedTotal`.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cupom aplicado.",
            content = @Content(schema = @Schema(implementation = CartResponse.class))),
        @ApiResponse(responseCode = "404", description = "Código de cupom não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"COUPON_NOT_FOUND","message":"Cupom não encontrado."}
                    """))),
        @ApiResponse(responseCode = "409", description = "Cupom expirado, esgotado ou não aplicável a este carrinho.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    CartResponse applyCoupon(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @Valid @RequestBody ApplyCouponRequest body);

    @Operation(summary = "Remove o cupom aplicado")
    @ApiResponse(responseCode = "200", description = "Cupom removido (idempotente).",
        content = @Content(schema = @Schema(implementation = CartResponse.class)))
    CartResponse removeCoupon(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me);
}
