package com.cardapio.catalog.api.rest;

import com.cardapio.catalog.api.dto.CategoryRequest;
import com.cardapio.catalog.api.dto.CategoryResponse;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Catalog — Categories (Admin)", description = "CRUD de categorias do menu. Requer role OWNER ou MANAGER.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface CategoryAdminApi {

    @Operation(summary = "Lista todas as categorias (ativas e inativas)")
    @ApiResponse(responseCode = "200", description = "Lista retornada.",
        content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(
            schema = @Schema(implementation = CategoryResponse.class))))
    List<CategoryResponse> list();

    @Operation(summary = "Cria uma nova categoria",
        description = "Retorna 201 com header `Location` apontando para o recurso criado.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Categoria criada.",
            content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validação falhou.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Sem permissão.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest req);

    @Operation(summary = "Atualiza uma categoria existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categoria atualizada.",
            content = @Content(schema = @Schema(implementation = CategoryResponse.class))),
        @ApiResponse(responseCode = "404", description = "Categoria não encontrada.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"CATEGORY_NOT_FOUND","message":"Categoria não encontrada."}
                    """)))
    })
    CategoryResponse update(
        @Parameter(description = "ID da categoria.", example = "9c5f50d2-7a02-4c41-bb9c-d56b9c92ec0a")
        @PathVariable UUID id,
        @Valid @RequestBody CategoryRequest req);

    @Operation(summary = "Remove uma categoria",
        description = "Categorias com produtos vinculados não podem ser removidas — mova-os ou apague-os primeiro.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Categoria removida."),
        @ApiResponse(responseCode = "409", description = "Categoria possui produtos vinculados.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"CATEGORY_NOT_EMPTY","message":"Não é possível remover uma categoria com produtos."}
                    """)))
    })
    ResponseEntity<Void> delete(
        @Parameter(description = "ID da categoria.", example = "9c5f50d2-7a02-4c41-bb9c-d56b9c92ec0a")
        @PathVariable UUID id);
}
