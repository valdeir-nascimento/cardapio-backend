package com.cardapio.promotion.api.dto;

import com.cardapio.promotion.application.command.CreateCouponCommand;
import com.cardapio.promotion.domain.model.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Cria um novo cupom de desconto.")
public record CreateCouponRequest(

    @Schema(description = "Código do cupom (case-insensitive). Único.",
        example = "BEMVINDO10", maxLength = 32, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank @Size(max = 32) String code,

    @Schema(description = "Tipo de desconto: percentual ou valor fixo.",
        example = "PERCENTAGE", allowableValues = {"PERCENTAGE", "FIXED"},
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull DiscountType type,

    @Schema(description = """
        Valor do desconto. Para PERCENTAGE: 1-100 (ex: 10 = 10% off).
        Para FIXED: reais (ex: 15.00 = R$ 15 off).
        """, example = "10.00", minimum = "0.01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull @DecimalMin(value = "0.01") BigDecimal value,

    @Schema(description = "Moeda ISO-4217. Default: BRL.", example = "BRL", nullable = true)
    @Size(min = 3, max = 3) String currency,

    @Schema(description = "Quando passa a valer (UTC). `null` = imediato.",
        example = "2026-05-01T00:00:00Z", format = "date-time", nullable = true)
    Instant validFrom,

    @Schema(description = "Quando expira (UTC). `null` = sem expiração.",
        example = "2026-12-31T23:59:59Z", format = "date-time", nullable = true)
    Instant validUntil,

    @Schema(description = "Pedido mínimo para aplicar o cupom.",
        example = "50.00", minimum = "0.00", nullable = true)
    @DecimalMin(value = "0.00") BigDecimal minOrderValue,

    @Schema(description = "Limite total de usos. `null` = ilimitado.", example = "100", nullable = true)
    Integer maxUses
) {
    public CreateCouponCommand toCommand() {
        return new CreateCouponCommand(
            code, type, value,
            currency == null || currency.isBlank() ? "BRL" : currency,
            validFrom, validUntil, minOrderValue, maxUses);
    }
}
