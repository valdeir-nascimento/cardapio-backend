package com.cardapio.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login via provedor social (Google ou Apple). Envia o ID Token obtido no app/web.")
public record SocialLoginRequest(

    @Schema(
        description = """
            ID Token (JWT) emitido pelo provedor de identidade após o login social no client.
            O backend valida a assinatura via JWKS público do provedor (Google/Apple) e
            extrai e-mail/nome para criar ou autenticar o cliente.
            """,
        example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjFhMmIzYzRkIn0.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJzdWIiOiIxMTAxNjkwODQ0MTM5OTEwNDAyMDAiLCJlbWFpbCI6ImpvYW8uc2lsdmFAZ21haWwuY29tIiwibmFtZSI6IkpvYW8gU2lsdmEiLCJleHAiOjE3NjI4MTI4MDB9.example-signature",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String idToken
) {}
