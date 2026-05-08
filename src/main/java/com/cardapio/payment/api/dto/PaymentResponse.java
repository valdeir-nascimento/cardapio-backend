package com.cardapio.payment.api.dto;

import com.cardapio.payment.application.dto.PaymentView;
import com.cardapio.payment.domain.model.PaymentMethod;
import com.cardapio.payment.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Estado completo de um pagamento. Use para polling enquanto Pix não confirma.")
public record PaymentResponse(

    @Schema(format = "uuid", example = "0c1d3a48-7b22-4ee9-a8d9-2c4e1f0a8c11") UUID id,

    @Schema(format = "uuid", example = "21a98176-44df-49eb-a37c-b8329f1e0123") UUID orderId,

    @Schema(example = "PIX") PaymentMethod method,

    @Schema(description = "Status atual.", example = "APPROVED") PaymentStatus status,

    @Schema(example = "98.40") BigDecimal amount,

    @Schema(example = "BRL") String currency,

    @Schema(nullable = true) String qrCode,

    @Schema(nullable = true) String qrCodeBase64,

    @Schema(example = "visa", nullable = true) String cardBrand,

    @Schema(example = "1234", nullable = true) String cardLast4,

    @Schema(description = "Motivo da falha quando `status=REJECTED`.",
        example = "card_rejected_insufficient_amount", nullable = true)
    String failureReason,

    @Schema(format = "date-time", example = "2026-05-07T20:30:00Z") Instant createdAt,

    @Schema(format = "date-time", example = "2026-05-07T20:31:14Z") Instant updatedAt
) {
    public static PaymentResponse from(PaymentView v) {
        return new PaymentResponse(
            v.id(), v.orderId(), v.method(), v.status(),
            v.amount(), v.currency(),
            v.qrCode(), v.qrCodeBase64(),
            v.cardBrand(), v.cardLast4(), v.failureReason(),
            v.createdAt(), v.updatedAt()
        );
    }
}
