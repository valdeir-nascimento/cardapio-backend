package com.cardapio.identity.api.rest;

import com.cardapio.identity.api.dto.SocialLoginRequest;
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

@Tag(name = "Auth — Social", description = "Login com provedor externo (Google, Apple).")
@SecurityRequirements
public interface SocialAuthApi {

    @Operation(
        summary = "Login com Google",
        description = """
            Recebe o `idToken` do Google obtido no client (Android/iOS/Web), valida a
            assinatura via JWKS público em `https://www.googleapis.com/oauth2/v3/certs`,
            confere o `audience` (client-id) e:

            - **se o e-mail já existe** → faz login no cliente correspondente
            - **se não existe** → cria um novo cliente automaticamente com nome/e-mail extraídos do token

            Sujeito a rate limit de **30 requisições/minuto** por IP.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login (ou cadastro implícito) bem-sucedido.",
            content = @Content(schema = @Schema(implementation = TokenPairResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "ID token inválido, expirado ou de aplicação não autorizada.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"INVALID_ID_TOKEN","message":"ID token do Google inválido ou expirado."}
                    """)))
    })
    TokenPairResponse google(@Valid @RequestBody SocialLoginRequest body);

    @Operation(
        summary = "Login com Apple",
        description = """
            Equivalente ao Google, mas usando JWKS da Apple
            (`https://appleid.apple.com/auth/keys`) e o `audience` do bundle do app iOS.
            Mesma semântica de login-ou-cadastro automático.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login bem-sucedido.",
            content = @Content(schema = @Schema(implementation = TokenPairResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "ID token inválido.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    TokenPairResponse apple(@Valid @RequestBody SocialLoginRequest body);
}
