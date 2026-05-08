package com.cardapio.catalog.api.rest;

import com.cardapio.catalog.api.dto.OperatingHoursResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Catalog — Operating Hours (Public)", description = "Horário de funcionamento exibido aos clientes.")
@SecurityRequirements
public interface PublicOperatingHoursApi {

    @Operation(
        summary = "Retorna o horário de funcionamento atual",
        description = "Endpoint público — usado pelo cliente para saber quando o restaurante está aberto.")
    @ApiResponse(
        responseCode = "200",
        description = "Horário retornado.",
        content = @Content(schema = @Schema(implementation = OperatingHoursResponse.class)))
    OperatingHoursResponse get();
}
