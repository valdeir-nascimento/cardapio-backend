package com.cardapio.identity.api.rest;

import com.cardapio.identity.api.dto.LoginRequest;
import com.cardapio.identity.api.dto.TokenPairResponse;
import com.cardapio.shared.openapi.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth — Admin", description = "Login do painel administrativo (OWNER, MANAGER, OPERATOR).")
@SecurityRequirements
public interface AdminAuthApi {

    @Operation(
        summary = "Login do administrador",
        description = """
            Autentica um usuário administrativo e devolve um par de tokens JWT.
            O `accessToken` carrega a role do usuário (`OWNER`, `MANAGER` ou `OPERATOR`),
            usada nos `@PreAuthorize` dos endpoints `/api/v1/admin/**`.

            Sujeito a rate limit de **10 requisições/minuto** por IP.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login bem-sucedido.",
            content = @Content(schema = @Schema(implementation = TokenPairResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciais inválidas ou usuário sem permissão administrativa.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"INVALID_CREDENTIALS","message":"E-mail ou senha inválidos."}
                    """))),
        @ApiResponse(
            responseCode = "429",
            description = "Rate limit excedido.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    TokenPairResponse login(@Valid @RequestBody LoginRequest req);
}
