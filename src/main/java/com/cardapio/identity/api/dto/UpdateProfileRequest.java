package com.cardapio.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(@NotBlank String name, @NotBlank String phoneNumber) {}
