package com.cardapio.ordering.api.rest;

import com.cardapio.ordering.api.dto.CreateTableRequest;
import com.cardapio.ordering.api.dto.TableQrResponse;
import com.cardapio.ordering.api.dto.TableResponse;
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

@Tag(name = "Ordering — Tables (Admin)", description = "CRUD de mesas e geração da imagem do QR Code para impressão.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface AdminTableApi {

    @Operation(summary = "Cria uma nova mesa",
        description = "Gera automaticamente o `qrToken` que será impresso no QR Code físico.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Mesa criada.",
            content = @Content(schema = @Schema(implementation = TableResponse.class))),
        @ApiResponse(responseCode = "409", description = "Já existe mesa com este `number`.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"TABLE_NUMBER_TAKEN","message":"Já existe uma mesa com este número."}
                    """)))
    })
    ResponseEntity<TableResponse> create(@Valid @RequestBody CreateTableRequest body);

    @Operation(summary = "Lista todas as mesas")
    @ApiResponse(responseCode = "200", description = "Mesas retornadas.")
    List<TableResponse> list();

    @Operation(summary = "Retorna a URL do QR Code da mesa para impressão",
        description = """
            Devolve uma URL assinada e temporária (TTL ~10 minutos) que serve a
            imagem PNG do QR Code da mesa. Use no navegador para imprimir.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL gerada.",
            content = @Content(schema = @Schema(implementation = TableQrResponse.class))),
        @ApiResponse(responseCode = "404", description = "Mesa não existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    TableQrResponse qr(
        @Parameter(description = "ID da mesa.", example = "f12a3c8e-1b4d-4e9c-90b1-aa7b3e2c45ab")
        @PathVariable UUID id);
}
