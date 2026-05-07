package com.cardapio.ordering.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplyCouponRequest(@NotBlank @Size(max = 32) String code) {}
