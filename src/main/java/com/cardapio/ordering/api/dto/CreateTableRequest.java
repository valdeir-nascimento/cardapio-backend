package com.cardapio.ordering.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Cria uma nova mesa do salão. O sistema gera automaticamente o token QR.")
public record CreateTableRequest(

    @Schema(description = "Número físico da mesa (visível ao cliente). Deve ser único.",
        example = "12", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull @Min(1) Integer number
) {}
