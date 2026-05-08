package com.cardapio.delivery.api.rest;

import com.cardapio.delivery.api.dto.NeighborhoodResponse;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

import static com.cardapio.delivery.api.rest.PublicDeliveryController.DeliveryFeeResponse;

@Tag(name = "Delivery — Public",
    description = "Bairros atendidos e cálculo de taxa para o cliente.")
@SecurityRequirements
public interface PublicDeliveryApi {

    @Operation(summary = "Lista os bairros atualmente atendidos",
        description = "Apenas bairros com `active=true`. Use no formulário de endereço do cliente.")
    @ApiResponse(responseCode = "200", description = "Bairros retornados.")
    List<NeighborhoodResponse> active();

    @Operation(summary = "Calcula a taxa de entrega para um bairro",
        description = "Retorna a taxa em vigor. Use antes do checkout para mostrar o valor ao cliente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Taxa calculada.",
            content = @Content(schema = @Schema(implementation = DeliveryFeeResponse.class),
                examples = @ExampleObject(value = "{\"fee\":10.00,\"currency\":\"BRL\"}"))),
        @ApiResponse(responseCode = "404", description = "Bairro não atendido ou inativo.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"NEIGHBORHOOD_NOT_SERVED","message":"Não atendemos este bairro."}
                    """)))
    })
    DeliveryFeeResponse fee(
        @Parameter(description = "ID do bairro do cliente (obtido em `/neighborhoods`).",
            example = "8b4e3d2a-c1f5-4321-9876-fedcba012345", required = true)
        @RequestParam UUID neighborhoodId);
}
