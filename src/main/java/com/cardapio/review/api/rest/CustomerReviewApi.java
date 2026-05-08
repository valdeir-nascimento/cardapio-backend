package com.cardapio.review.api.rest;

import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.review.api.dto.ReviewResponse;
import com.cardapio.review.api.dto.ReviewableOrderResponse;
import com.cardapio.review.api.dto.SubmitReviewRequest;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(name = "Review — Customer",
    description = "Avaliação de pedidos pelo cliente. Cada pedido finalizado pode receber uma avaliação.")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
public interface CustomerReviewApi {

    @Operation(summary = "Lista pedidos do cliente que ainda não foram avaliados",
        description = "Apenas pedidos em estado terminal (DELIVERED ou CANCELLED) que ainda não têm review.")
    @ApiResponse(responseCode = "200", description = "Lista retornada.")
    List<ReviewableOrderResponse> reviewable(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset);

    @Operation(summary = "Envia avaliação de um pedido",
        description = """
            Cada pedido pode receber **apenas uma** avaliação. Tentativa de
            avaliar pedido já avaliado retorna 409.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Avaliação registrada.",
            content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
        @ApiResponse(responseCode = "404", description = "Pedido não pertence ao cliente.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Pedido ainda não está em estado terminal ou já foi avaliado.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"REVIEW_ALREADY_EXISTS","message":"Este pedido já foi avaliado."}
                    """)))
    })
    ResponseEntity<ReviewResponse> submit(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @Parameter(description = "ID do pedido.", example = "21a98176-44df-49eb-a37c-b8329f1e0123")
        @PathVariable UUID id,
        @Valid @RequestBody SubmitReviewRequest body);

    @Operation(summary = "Recupera a avaliação que o cliente fez deste pedido")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Avaliação retornada.",
            content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
        @ApiResponse(responseCode = "404", description = "Sem avaliação para este pedido.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    ReviewResponse getMine(
        @Parameter(hidden = true) @AuthenticationPrincipal CardapioPrincipal me,
        @PathVariable UUID id);
}
