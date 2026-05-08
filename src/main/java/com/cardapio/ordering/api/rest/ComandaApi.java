package com.cardapio.ordering.api.rest;

import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.ordering.api.dto.ComandaResponse;
import com.cardapio.ordering.api.dto.OpenComandaRequest;
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

@Tag(name = "Ordering — Comandas (Customer)",
    description = "Comandas compartilhadas — múltiplos clientes na mesma mesa lançam pedidos juntos.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface ComandaApi {

    @Operation(summary = "Abre uma nova comanda em uma mesa",
        description = """
            O cliente que abre torna-se o primeiro membro. Outros clientes podem
            se juntar via `POST /comandas/{id}/join`. Só pode haver uma comanda
            aberta por mesa.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Comanda aberta.",
            content = @Content(schema = @Schema(implementation = ComandaResponse.class))),
        @ApiResponse(responseCode = "404", description = "Mesa não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Já existe comanda aberta nesta mesa.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"COMANDA_ALREADY_OPEN","message":"Esta mesa já tem uma comanda aberta."}
                    """)))
    })
    ResponseEntity<ComandaResponse> open(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @Valid @RequestBody OpenComandaRequest body);

    @Operation(summary = "Junta o cliente logado a uma comanda existente",
        description = "Após entrar, o cliente pode lançar pedidos vinculados à comanda.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente adicionado.",
            content = @Content(schema = @Schema(implementation = ComandaResponse.class))),
        @ApiResponse(responseCode = "404", description = "Comanda não existe ou está fechada.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ComandaResponse join(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @Parameter(description = "ID da comanda.", example = "07e2b8d8-cc88-4d4d-9311-ec0a0c6a2af0")
        @PathVariable UUID id);

    @Operation(summary = "Detalhes da comanda",
        description = "Apenas membros da comanda podem consultar.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comanda retornada.",
            content = @Content(schema = @Schema(implementation = ComandaResponse.class))),
        @ApiResponse(responseCode = "403", description = "Cliente não é membro desta comanda.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"COMANDA_NOT_MEMBER","message":"Você não é membro desta comanda."}
                    """))),
        @ApiResponse(responseCode = "404", description = "Comanda não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ComandaResponse get(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @PathVariable UUID id);
}
