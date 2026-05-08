package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.TableQrView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.time.Instant;

@Schema(description = "URL assinada e temporária para baixar a imagem PNG do QR Code da mesa.")
public record TableQrResponse(

    @Schema(description = "URL pública pronta para usar em `<img src>`. Inclui assinatura HMAC + expiração.",
        example = "http://localhost:8080/api/v1/qr/tables/f12a3c8e-1b4d-4e9c-90b1-aa7b3e2c45ab.png?exp=1762812800&sig=abc123",
        format = "uri")
    URI presignedUrl,

    @Schema(description = "Quando a URL expira. Após isso retorna 410 Gone.",
        format = "date-time", example = "2026-05-07T20:40:00Z")
    Instant expiresAt
) {
    public static TableQrResponse from(TableQrView v) {
        return new TableQrResponse(v.presignedUrl(), v.expiresAt());
    }
}
