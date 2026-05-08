package com.cardapio.catalog.api.rest;

import com.cardapio.catalog.api.dto.OperatingHoursRequest;
import com.cardapio.shared.openapi.ApiErrorResponse;
import com.cardapio.shared.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Catalog — Operating Hours (Admin)", description = "Edição do horário de funcionamento do estabelecimento.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface OperatingHoursAdminApi {

    @Operation(summary = "Substitui completamente o horário de funcionamento",
        description = """
            Operação de upsert atômico — envie o mapa completo. Dias ausentes do payload
            ficam configurados como **fechado**.

            Validações:
            - `closeTime` deve ser maior que `openTime` em cada faixa
            - faixas do mesmo dia não podem se sobrepor
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Horário atualizado."),
        @ApiResponse(responseCode = "400", description = "Faixa inválida (sobreposição ou close <= open).",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ResponseEntity<Void> updateAll(@Valid @RequestBody OperatingHoursRequest req);
}
