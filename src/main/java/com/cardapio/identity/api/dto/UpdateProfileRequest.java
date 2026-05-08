package com.cardapio.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Campos editáveis do perfil do cliente. E-mail não é alterável por aqui.")
public record UpdateProfileRequest(

    @Schema(
        description = "Novo nome completo.",
        example = "João da Silva Junior",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String name,

    @Schema(
        description = "Novo telefone em E.164.",
        example = "+5511988887777",
        requiredMode = Schema.RequiredMode.REQUIRED,
        pattern = "^\\+[1-9]\\d{1,14}$")
    @NotBlank String phoneNumber
) {}
