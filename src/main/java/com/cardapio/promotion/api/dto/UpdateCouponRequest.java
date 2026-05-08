package com.cardapio.promotion.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = """
    Edição parcial de um cupom. Campos `null` mantêm o valor anterior.
    Não permite trocar `code` ou `type` (crie um cupom novo se precisar).
    """)
public record UpdateCouponRequest(

    @Schema(description = "Novo valor do desconto.", example = "15.00", minimum = "0.01", nullable = true)
    @DecimalMin(value = "0.01") BigDecimal value,

    @Schema(description = "Nova data de expiração (UTC).",
        example = "2027-12-31T23:59:59Z", format = "date-time", nullable = true)
    Instant validUntil,

    @Schema(description = "Novo valor mínimo do pedido.", example = "60.00", minimum = "0.00", nullable = true)
    @DecimalMin(value = "0.00") BigDecimal minOrderValue,

    @Schema(description = "Novo limite total de usos.", example = "200", minimum = "0", nullable = true)
    @Min(0) Integer maxUses
) {}
