package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.domain.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record AdvanceStatusRequest(@NotNull OrderStatus status) {
}
