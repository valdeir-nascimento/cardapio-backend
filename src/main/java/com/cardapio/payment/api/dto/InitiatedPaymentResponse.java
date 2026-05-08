package com.cardapio.payment.api.dto;

import com.cardapio.payment.application.dto.InitiatedPaymentView;
import com.cardapio.payment.domain.model.PaymentMethod;
import com.cardapio.payment.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = """
    Pagamento recém-iniciado. Para Pix traz `qrCode` (copia-e-cola) e `qrCodeBase64` (imagem).
    Para cartão traz `cardBrand` e `cardLast4` para exibição.
    """)
public record InitiatedPaymentResponse(

    @Schema(format = "uuid", example = "0c1d3a48-7b22-4ee9-a8d9-2c4e1f0a8c11") UUID id,

    @Schema(example = "PIX") PaymentMethod method,

    @Schema(description = "PENDING para Pix (aguarda pagamento) ou APPROVED/REJECTED imediato para cartão.",
        example = "PENDING") PaymentStatus status,

    @Schema(description = "Valor cobrado em reais.", example = "98.40") BigDecimal amount,

    @Schema(example = "BRL") String currency,

    @Schema(description = "Pix copia-e-cola — string EMV. `null` para cartão.",
        example = "00020101021126830014br.gov.bcb.pix0136abc...",
        nullable = true)
    String qrCode,

    @Schema(description = "Pix imagem PNG em Base64. `null` para cartão.",
        example = "iVBORw0KGgoAAAANSUhEUgAAA...", nullable = true)
    String qrCodeBase64,

    @Schema(description = "Bandeira do cartão (apenas CARD).", example = "visa", nullable = true)
    String cardBrand,

    @Schema(description = "Últimos 4 dígitos do cartão.", example = "1234", nullable = true)
    String cardLast4
) {
    public static InitiatedPaymentResponse from(InitiatedPaymentView v) {
        return new InitiatedPaymentResponse(
            v.id(), v.method(), v.status(),
            v.amount(), v.currency(),
            v.qrCode(), v.qrCodeBase64(),
            v.cardBrand(), v.cardLast4()
        );
    }
}
