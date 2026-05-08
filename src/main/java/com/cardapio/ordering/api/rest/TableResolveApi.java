package com.cardapio.ordering.api.rest;

import com.cardapio.ordering.api.dto.ResolveTableResponse;
import com.cardapio.shared.openapi.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Ordering — Table Resolve",
    description = "Resolve um token QR para a mesa correspondente. Endpoint público (cliente lê o QR antes de logar).")
@SecurityRequirements
public interface TableResolveApi {

    @Operation(summary = "Resolve token QR para mesa",
        description = """
            O QR Code físico codifica uma URL como `https://app.cardapio.com/?table=<token>`.
            O app/site usa este endpoint para descobrir qual mesa está atrás do token e
            se já tem comanda aberta — daí decide se exibe "Abrir comanda" ou "Entrar na comanda existente".
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mesa identificada.",
            content = @Content(schema = @Schema(implementation = ResolveTableResponse.class))),
        @ApiResponse(responseCode = "404", description = "Token não corresponde a nenhuma mesa ativa.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"TABLE_NOT_FOUND","message":"Mesa não encontrada para este QR."}
                    """)))
    })
    ResolveTableResponse resolve(
        @Parameter(description = "Token público da mesa, lido do QR Code.",
            example = "8e7c2f44-5b1a-49d1-ae9c-3a18d0b62e44", required = true)
        @RequestParam UUID token);
}
