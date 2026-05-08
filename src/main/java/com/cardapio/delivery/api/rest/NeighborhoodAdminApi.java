package com.cardapio.delivery.api.rest;

import com.cardapio.delivery.api.dto.NeighborhoodRequest;
import com.cardapio.delivery.api.dto.NeighborhoodResponse;
import com.cardapio.shared.openapi.ApiErrorResponse;
import com.cardapio.shared.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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

@Tag(name = "Delivery — Neighborhoods (Admin)",
    description = "Gestão dos bairros atendidos com taxa fixa por bairro.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface NeighborhoodAdminApi {

    @Operation(summary = "Lista todos os bairros (ativos e inativos)")
    @ApiResponse(responseCode = "200", description = "Lista retornada.")
    List<NeighborhoodResponse> list();

    @Operation(summary = "Cadastra um novo bairro atendido")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Bairro criado.",
            content = @Content(schema = @Schema(implementation = NeighborhoodResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validação falhou (taxa negativa, etc).",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ResponseEntity<NeighborhoodResponse> create(@Valid @RequestBody NeighborhoodRequest req);

    @Operation(summary = "Atualiza dados e taxa de um bairro")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bairro atualizado."),
        @ApiResponse(responseCode = "404", description = "Bairro não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    NeighborhoodResponse update(
        @Parameter(description = "ID do bairro.", example = "8b4e3d2a-c1f5-4321-9876-fedcba012345")
        @PathVariable UUID id,
        @Valid @RequestBody NeighborhoodRequest req);

    @Operation(summary = "Remove um bairro do delivery",
        description = "Pedidos antigos para este bairro são preservados.")
    @ApiResponse(responseCode = "204", description = "Bairro removido.")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
