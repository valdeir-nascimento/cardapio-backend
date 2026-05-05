package com.cardapio.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CategoryRequest(@NotBlank String name, @PositiveOrZero int displayOrder, Boolean active) {}
