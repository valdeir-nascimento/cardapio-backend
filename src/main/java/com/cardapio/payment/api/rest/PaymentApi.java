package com.cardapio.payment.api.rest;

import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.payment.api.dto.InitiatePaymentRequest;
import com.cardapio.payment.api.dto.InitiatedPaymentResponse;
import com.cardapio.payment.api.dto.PaymentResponse;
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

@Tag(name = "Payment", description = "Pagamentos via Mercado Pago (Pix e cartão).")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface PaymentApi {

    @Operation(summary = "Inicia o pagamento de um pedido",
        description = """
            ### Pix
            Retorna `qrCode` (string EMV "copia-e-cola") e `qrCodeBase64` (imagem PNG).
            Status fica `PENDING` até o pagador concluir; o webhook do Mercado Pago atualiza
            para `APPROVED` automaticamente.

            ### Cartão
            Exige `cardToken` previamente gerado pelo SDK do Mercado Pago no front-end.
            Retorno é imediato (`APPROVED` ou `REJECTED`).

            ### Idempotência
            Tentar iniciar pagamento num pedido que já tem pagamento ativo retorna `409
            PAYMENT_ALREADY_INITIATED`. Cancele/refunde o pagamento atual antes de iniciar outro.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Pagamento iniciado.",
            content = @Content(schema = @Schema(implementation = InitiatedPaymentResponse.class))),
        @ApiResponse(responseCode = "400", description = "`cardToken` faltando para CARD.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"CARD_TOKEN_REQUIRED","message":"cardToken obrigatório para pagamento por cartão."}
                    """))),
        @ApiResponse(responseCode = "404", description = "Pedido não existe ou não pertence ao cliente.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Já existe pagamento ativo para este pedido.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"PAYMENT_ALREADY_INITIATED","message":"Já existe pagamento ativo para este pedido."}
                    """)))
    })
    ResponseEntity<InitiatedPaymentResponse> initiate(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @Parameter(description = "ID do pedido.", example = "21a98176-44df-49eb-a37c-b8329f1e0123")
        @PathVariable UUID orderId,
        @Valid @RequestBody InitiatePaymentRequest body);

    @Operation(summary = "Consulta status do pagamento",
        description = "Use para fazer polling enquanto aguarda confirmação Pix.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pagamento retornado.",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Pagamento não pertence ao cliente.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    PaymentResponse get(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @Parameter(description = "ID do pagamento.", example = "0c1d3a48-7b22-4ee9-a8d9-2c4e1f0a8c11")
        @PathVariable UUID id);
}
