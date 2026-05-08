package com.cardapio.ordering.api.rest;

import com.cardapio.ordering.api.dto.ComandaResponse;
import com.cardapio.ordering.domain.model.ComandaStatus;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(name = "Ordering — Comandas (Admin)", description = "Listagem e fechamento de comandas pelo painel.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface AdminComandaApi {

    @Operation(summary = "Lista comandas filtradas por status",
        description = "Default: comandas abertas. Use `?status=CLOSED` para histórico.")
    @ApiResponse(responseCode = "200", description = "Comandas retornadas.")
    List<ComandaResponse> list(
        @Parameter(description = "Status a filtrar.", example = "OPEN")
        @RequestParam(defaultValue = "OPEN") ComandaStatus status);

    @Operation(summary = "Fecha uma comanda",
        description = """
            Marca a comanda como `CLOSED`. Não altera os pedidos vinculados —
            eles continuam seu fluxo normal de status.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Comanda fechada.",
            content = @Content(schema = @Schema(implementation = ComandaResponse.class))),
        @ApiResponse(responseCode = "404", description = "Comanda não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Comanda já estava fechada.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ComandaResponse close(
        @Parameter(description = "ID da comanda.", example = "07e2b8d8-cc88-4d4d-9311-ec0a0c6a2af0")
        @PathVariable UUID id);
}
