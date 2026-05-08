package com.cardapio.payment.api.dto;

import com.cardapio.payment.application.command.InitiatePaymentCommand;
import com.cardapio.payment.domain.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Inicia o pagamento de um pedido. Pix gera QR Code; cartão exige `cardToken` previamente tokenizado.")
public record InitiatePaymentRequest(

    @Schema(description = "Método de pagamento.", example = "PIX",
        allowableValues = {"PIX", "CARD"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull PaymentMethod method,

    @Schema(description = """
        Token do cartão (obrigatório quando `method=CARD`).
        Gerado no front-end pela Public Key do Mercado Pago — o backend nunca
        vê o número do cartão real (PCI-compliance).
        """, example = "65f8f2c2c8e4a8b9d6e2f1a3", nullable = true)
    String cardToken
) {
    public InitiatePaymentCommand toCommand(UUID orderId, UUID customerId) {
        return new InitiatePaymentCommand(orderId, customerId, method, cardToken);
    }
}
