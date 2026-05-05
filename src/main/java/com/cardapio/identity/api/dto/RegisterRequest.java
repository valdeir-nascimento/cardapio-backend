package com.cardapio.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank String name,
    @NotBlank String email,
    @NotBlank String phoneNumber,
    @NotBlank String password
) {}
