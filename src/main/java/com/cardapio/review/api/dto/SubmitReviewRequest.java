package com.cardapio.review.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Avaliação de um pedido finalizado pelo cliente.")
public record SubmitReviewRequest(

    @Schema(description = "Nota de 1 a 5 estrelas.", example = "5",
        minimum = "1", maximum = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull @Min(1) @Max(5) Integer rating,

    @Schema(description = "Comentário opcional (até 500 caracteres).",
        example = "Pizza chegou quentinha e muito saborosa! Entrega rápida.",
        maxLength = 500, nullable = true)
    @Size(max = 500) String comment
) {}
