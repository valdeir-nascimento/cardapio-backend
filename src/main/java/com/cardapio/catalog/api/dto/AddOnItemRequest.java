package com.cardapio.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddOnItemRequest(@NotBlank String name, @NotNull BigDecimal price) {}
