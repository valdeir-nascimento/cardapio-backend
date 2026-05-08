package com.cardapio.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais para login com e-mail e senha.")
public record LoginRequest(

    @Schema(
        description = "E-mail cadastrado. Único por usuário.",
        example = "joao.silva@email.com",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String email,

    @Schema(
        description = "Senha em texto puro. Comparada contra o hash BCrypt armazenado.",
        example = "MinhaSenh@123",
        requiredMode = Schema.RequiredMode.REQUIRED,
        format = "password")
    @NotBlank String password
) {}
