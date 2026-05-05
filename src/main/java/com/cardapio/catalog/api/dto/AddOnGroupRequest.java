package com.cardapio.catalog.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record AddOnGroupRequest(
    @NotBlank String name,
    @PositiveOrZero int minSelection,
    @PositiveOrZero int maxSelection,
    @NotNull @Valid List<AddOnItemRequest> items
) {}
