package com.cardapio.identity.api.rest;

import com.cardapio.identity.api.dto.ProfileResponse;
import com.cardapio.identity.api.dto.UpdateProfileRequest;
import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.identity.application.dto.CustomerDataExport;
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
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Customer — Me", description = "Perfil, edição e LGPD do cliente autenticado.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface MeApi {

    @Operation(
        summary = "Retorna o perfil do cliente logado",
        description = "Inclui o cliente identificado pelo `subject` do JWT.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Perfil retornado.",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class),
                examples = @ExampleObject(value = """
                    {"id":"c2b41216-14f8-4d2c-a0d8-34b97dc7b897","name":"João da Silva","email":"joao.silva@email.com","phoneNumber":"+5511999998888"}
                    """))),
        @ApiResponse(responseCode = "401", description = "JWT ausente ou inválido.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Token sem role CUSTOMER.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ProfileResponse me(@Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal principal);

    @Operation(
        summary = "Atualiza nome e telefone do cliente logado",
        description = "Não permite alterar e-mail (que é usado como chave de login).")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Perfil atualizado.",
            content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validação falhou.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ProfileResponse update(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal principal,
        @Valid @RequestBody UpdateProfileRequest req);

    @Operation(
        summary = "Exclui a própria conta (direito de exclusão LGPD)",
        description = """
            Marca o cliente como deletado e anonimiza dados pessoais
            (nome → "Cliente removido", e-mail/telefone → tokens aleatórios).
            Pedidos passados são preservados para fins fiscais, mas não vinculam
            mais a uma identidade real.

            Operação irreversível.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Conta removida."),
        @ApiResponse(responseCode = "401", description = "Não autenticado.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    void delete(@Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal principal);

    @Operation(
        summary = "Exporta todos os dados do cliente em JSON (direito de portabilidade LGPD)",
        description = """
            Retorna um arquivo JSON com perfil, pedidos, comandas, reviews e
            preferências do cliente. O Content-Disposition vem como `attachment`
            para o navegador baixar o arquivo automaticamente.
            """)
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Arquivo JSON com export completo.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerDataExport.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ResponseEntity<CustomerDataExport> dataExport(@Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal principal);
}
