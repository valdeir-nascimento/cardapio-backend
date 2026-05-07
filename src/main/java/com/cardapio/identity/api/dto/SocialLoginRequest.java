package com.cardapio.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(@NotBlank String idToken) {}
