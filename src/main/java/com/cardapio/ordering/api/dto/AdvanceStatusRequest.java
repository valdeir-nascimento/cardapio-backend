package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Move o pedido para o próximo status no fluxo. Transições inválidas retornam 409.")
public record AdvanceStatusRequest(

    @Schema(description = "Novo status. Geralmente CONFIRMED → PREPARING → READY → DELIVERING → DELIVERED.",
        example = "PREPARING", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull OrderStatus status
) {}
