package com.cardapio.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para cadastro de um novo cliente.")
public record RegisterRequest(

    @Schema(
        description = "Nome completo do cliente.",
        example = "João da Silva",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String name,

    @Schema(
        description = "E-mail de contato. Deve ser único no sistema.",
        example = "joao.silva@email.com",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String email,

    @Schema(
        description = "Telefone em formato E.164 (com código do país e DDD).",
        example = "+5511999998888",
        requiredMode = Schema.RequiredMode.REQUIRED,
        pattern = "^\\+[1-9]\\d{1,14}$")
    @NotBlank String phoneNumber,

    @Schema(
        description = "Senha em texto puro. Mínimo recomendado: 8 caracteres com letras e números.",
        example = "MinhaSenh@123",
        requiredMode = Schema.RequiredMode.REQUIRED,
        format = "password",
        minLength = 8)
    @NotBlank String password
) {}
