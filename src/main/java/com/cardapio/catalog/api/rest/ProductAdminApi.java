package com.cardapio.catalog.api.rest;

import com.cardapio.catalog.api.dto.ProductRequest;
import com.cardapio.catalog.api.dto.SetAvailabilityRequest;
import com.cardapio.catalog.api.dto.SetStockRequest;
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

import java.util.Map;
import java.util.UUID;

@Tag(name = "Catalog — Products (Admin)", description = "CRUD de produtos, controle de estoque e disponibilidade.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface ProductAdminApi {

    @Operation(summary = "Cria um novo produto",
        description = "Inclui variações e adicionais em uma única chamada. Retorna 201 com o ID do produto criado.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Produto criado.",
            content = @Content(schema = @Schema(example = "{\"id\":\"ad4f3c1b-7a02-4c41-bb9c-d56b9c92ec0a\"}"))),
        @ApiResponse(responseCode = "404", description = "`categoryId` não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ResponseEntity<Map<String, UUID>> create(@Valid @RequestBody ProductRequest req);

    @Operation(summary = "Atualiza um produto",
        description = "Substitui completamente as variações e adicionais — envie sempre a lista completa.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto atualizado."),
        @ApiResponse(responseCode = "404", description = "Produto não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Map<String, UUID> update(
        @Parameter(description = "ID do produto.", example = "ad4f3c1b-7a02-4c41-bb9c-d56b9c92ec0a")
        @PathVariable UUID id,
        @Valid @RequestBody ProductRequest req);

    @Operation(summary = "Liga/desliga a disponibilidade de um produto",
        description = "Use para ocultar temporariamente um item esgotado sem precisar editar o produto inteiro.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Disponibilidade alterada."),
        @ApiResponse(responseCode = "404", description = "Produto não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ResponseEntity<Void> availability(@PathVariable UUID id, @RequestBody SetAvailabilityRequest req);

    @Operation(summary = "Atualiza o estoque do produto",
        description = "Define quantidade absoluta. `null` desativa o controle de estoque para o item.")
    @ApiResponse(responseCode = "204", description = "Estoque atualizado.")
    ResponseEntity<Void> stock(@PathVariable UUID id, @RequestBody SetStockRequest req);

    @Operation(summary = "Remove um produto",
        description = "Soft-delete: marca como inativo. Pedidos passados continuam referenciando o produto histórico.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Produto removido."),
        @ApiResponse(responseCode = "404", description = "Produto não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"PRODUCT_NOT_FOUND","message":"Produto não encontrado."}
                    """)))
    })
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
