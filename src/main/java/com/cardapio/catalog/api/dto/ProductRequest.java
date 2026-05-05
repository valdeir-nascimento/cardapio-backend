package com.cardapio.catalog.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductRequest(
    @NotBlank String name,
    String description,
    @NotNull @Positive BigDecimal basePrice,
    @NotNull UUID categoryId,
    String imageUrl,
    boolean allowsHalfHalf,
    @Valid List<VariationRequest> variations,
    @Valid List<AddOnGroupRequest> addOnGroups
) {}
