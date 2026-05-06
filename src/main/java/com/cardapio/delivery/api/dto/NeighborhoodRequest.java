package com.cardapio.delivery.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record NeighborhoodRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 120) String city,
    @NotNull @DecimalMin("0.00") BigDecimal fee,
    Boolean active
) {}
