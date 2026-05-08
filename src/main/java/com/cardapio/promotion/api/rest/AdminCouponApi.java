package com.cardapio.promotion.api.rest;

import com.cardapio.promotion.api.dto.CouponResponse;
import com.cardapio.promotion.api.dto.CreateCouponRequest;
import com.cardapio.promotion.api.dto.UpdateCouponRequest;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Promotion — Coupons (Admin)",
    description = "Gestão de cupons de desconto. Aplicação no carrinho fica em `PATCH /api/v1/cart/coupon`.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface AdminCouponApi {

    @Operation(summary = "Cria um novo cupom de desconto")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cupom criado.",
            content = @Content(schema = @Schema(implementation = CouponResponse.class))),
        @ApiResponse(responseCode = "409", description = "Já existe cupom com este `code`.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"COUPON_CODE_TAKEN","message":"Já existe um cupom com este código."}
                    """)))
    })
    ResponseEntity<CouponResponse> create(@Valid @RequestBody CreateCouponRequest body);

    @Operation(summary = "Edita parcialmente um cupom",
        description = "Apenas valor, expiração, mínimo de pedido e limite de uso podem ser alterados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cupom atualizado.",
            content = @Content(schema = @Schema(implementation = CouponResponse.class))),
        @ApiResponse(responseCode = "404", description = "Cupom não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    CouponResponse update(
        @Parameter(description = "ID do cupom.", example = "fab12345-cccc-4321-9876-abcdef012345")
        @PathVariable UUID id,
        @Valid @RequestBody UpdateCouponRequest body);

    @Operation(summary = "Desativa um cupom (operação reversível? não — crie um novo)",
        description = "Após desativar, novos pedidos não podem mais aplicar este código. Pedidos antigos continuam válidos.")
    @ApiResponse(responseCode = "200", description = "Cupom desativado.",
        content = @Content(schema = @Schema(implementation = CouponResponse.class)))
    CouponResponse deactivate(@PathVariable UUID id);

    @Operation(summary = "Lista cupons com filtros opcionais")
    @ApiResponse(responseCode = "200", description = "Lista retornada.")
    List<CouponResponse> list(
        @Parameter(description = "Apenas ativos (não expirados, não desativados).", example = "false")
        @RequestParam(defaultValue = "false") boolean activeOnly,
        @Parameter(description = "Busca por código (substring case-insensitive).", example = "BEMVINDO")
        @RequestParam(required = false) String code,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset);
}
