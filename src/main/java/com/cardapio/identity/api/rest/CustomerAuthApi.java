package com.cardapio.identity.api.rest;

import com.cardapio.identity.api.dto.LoginRequest;
import com.cardapio.identity.api.dto.RefreshRequest;
import com.cardapio.identity.api.dto.RegisterRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Tag(name = "Auth — Customer", description = "Cadastro, login e renovação de token para clientes finais.")
@SecurityRequirements // public endpoints — no JWT required
public interface CustomerAuthApi {

    @Operation(
        summary = "Cadastra um novo cliente",
        description = """
            Cria uma conta de cliente final. O e-mail precisa ser único; se já existir,
            retorna 409 com `code=EMAIL_ALREADY_REGISTERED`.

            A senha é hashada com BCrypt antes de persistir — o backend nunca armazena
            o texto puro. Após o cadastro, o cliente deve fazer login normalmente para
            obter os tokens.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Cliente criado. O header `Location` aponta para `/api/v1/me`.",
            content = @Content(
                schema = @Schema(example = "{\"id\":\"c2b41216-14f8-4d2c-a0d8-34b97dc7b897\"}"))),
        @ApiResponse(
            responseCode = "400",
            description = "Payload inválido (campos faltando ou em formato incorreto).",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"VALIDATION_ERROR","message":"Campos inválidos","details":{"email":"deve ser um e-mail válido"}}
                    """))),
        @ApiResponse(
            responseCode = "409",
            description = "E-mail já cadastrado por outro cliente.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"EMAIL_ALREADY_REGISTERED","message":"Já existe um cliente cadastrado com este e-mail."}
                    """)))
    })
    ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req);

    @Operation(
        summary = "Login do cliente com e-mail e senha",
        description = """
            Autentica e devolve um par de tokens (access + refresh).
            Sujeito a rate limit de **10 requisições/minuto** por IP.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Login bem-sucedido.",
            content = @Content(schema = @Schema(implementation = TokenPairResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "E-mail e/ou senha incorretos.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"INVALID_CREDENTIALS","message":"E-mail ou senha inválidos."}
                    """))),
        @ApiResponse(
            responseCode = "429",
            description = "Excedeu o limite de tentativas — tente novamente em 1 minuto.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    TokenPairResponse login(@Valid @RequestBody LoginRequest req);

    @Operation(
        summary = "Renova o par de tokens usando o refresh token",
        description = """
            Recebe um `refreshToken` válido e devolve um novo par. O refresh anterior
            é invalidado (rotação) — guarde só o novo. Use sempre que o `accessToken`
            estiver perto de expirar.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Novo par emitido.",
            content = @Content(schema = @Schema(implementation = TokenPairResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Refresh token inválido, expirado ou já rotacionado.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"INVALID_REFRESH_TOKEN","message":"Refresh token inválido ou expirado."}
                    """)))
    })
    TokenPairResponse refresh(@Valid @RequestBody RefreshRequest req);
}
