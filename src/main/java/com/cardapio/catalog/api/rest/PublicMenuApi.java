package com.cardapio.catalog.api.rest;

import com.cardapio.catalog.api.dto.MenuResponse;
import com.cardapio.catalog.api.dto.ProductDetailsResponse;
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
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Tag(name = "Catalog — Public Menu", description = "Menu público consumido pelo app/site dos clientes.")
@SecurityRequirements
public interface PublicMenuApi {

    @Operation(
        summary = "Retorna o menu público",
        description = """
            Lista todas as categorias ativas com seus produtos disponíveis.
            Variações e adicionais NÃO vêm aqui — para detalhes use
            `GET /api/v1/menu/products/{id}`.

            Endpoint público, sem autenticação. Pode ser servido com cache HTTP.
            """)
    @ApiResponse(
        responseCode = "200",
        description = "Menu retornado.",
        content = @Content(schema = @Schema(implementation = MenuResponse.class)))
    MenuResponse menu();

    @Operation(
        summary = "Detalhes completos de um produto",
        description = "Inclui variações, adicionais, estoque e flag de meia-meia.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Produto encontrado.",
            content = @Content(schema = @Schema(implementation = ProductDetailsResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Produto não existe ou está inativo.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"PRODUCT_NOT_FOUND","message":"Produto não encontrado."}
                    """)))
    })
    ProductDetailsResponse product(
        @Parameter(description = "ID do produto.", example = "ad4f3c1b-7a02-4c41-bb9c-d56b9c92ec0a")
        @PathVariable UUID id
    );
}
