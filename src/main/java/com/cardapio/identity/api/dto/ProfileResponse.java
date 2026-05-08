package com.cardapio.identity.api.dto;

import com.cardapio.identity.application.dto.CustomerProfile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Dados públicos do cliente autenticado.")
public record ProfileResponse(

    @Schema(
        description = "ID interno do cliente (UUID v4).",
        example = "c2b41216-14f8-4d2c-a0d8-34b97dc7b897",
        format = "uuid")
    UUID id,

    @Schema(
        description = "Nome completo cadastrado.",
        example = "João da Silva")
    String name,

    @Schema(
        description = "E-mail de contato.",
        example = "joao.silva@email.com")
    String email,

    @Schema(
        description = "Telefone em E.164.",
        example = "+5511999998888")
    String phoneNumber
) {
    public static ProfileResponse from(CustomerProfile p) {
        return new ProfileResponse(p.id().value(), p.name(), p.email(), p.phoneNumber());
    }
}
