package com.cardapio.ordering.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateCartItemRequest(@Min(1) int quantity, @Size(max = 200) String observation) {
}
