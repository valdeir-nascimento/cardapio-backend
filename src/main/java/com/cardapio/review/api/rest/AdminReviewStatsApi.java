package com.cardapio.review.api.rest;

import com.cardapio.review.api.dto.ProductRatingStatsResponse;
import com.cardapio.shared.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Review — Stats (Admin)", description = "Estatísticas agregadas de avaliações por produto.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface AdminReviewStatsApi {

    @Operation(summary = "Lista estatísticas de avaliações por produto",
        description = """
            Retorna média e contagem de reviews por produto. Útil para o painel
            identificar campeões/perdedores. Filtros são compostos:
            sem `productId` → todos os produtos; com janela `from/to` → restringe ao período.
            """)
    @ApiResponse(responseCode = "200", description = "Estatísticas retornadas.")
    List<ProductRatingStatsResponse> productStats(
        @Parameter(description = "Restringe a um único produto.", example = "ad4f3c1b-7a02-4c41-bb9c-d56b9c92ec0a")
        @RequestParam(required = false) UUID productId,
        @Parameter(description = "Limite inferior de `createdAt` (UTC).", example = "2026-01-01T00:00:00Z")
        @RequestParam(required = false) Instant from,
        @Parameter(description = "Limite superior de `createdAt`.", example = "2026-12-31T23:59:59Z")
        @RequestParam(required = false) Instant to,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset);
}
