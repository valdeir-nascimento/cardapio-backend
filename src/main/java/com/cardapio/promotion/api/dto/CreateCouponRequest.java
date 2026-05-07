package com.cardapio.promotion.api.dto;

import com.cardapio.promotion.application.command.CreateCouponCommand;
import com.cardapio.promotion.domain.model.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCouponRequest(
    @NotBlank @Size(max = 32) String code,
    @NotNull DiscountType type,
    @NotNull @DecimalMin(value = "0.01") BigDecimal value,
    @Size(min = 3, max = 3) String currency,
    Instant validFrom,
    Instant validUntil,
    @DecimalMin(value = "0.00") BigDecimal minOrderValue,
    Integer maxUses
) {
    public CreateCouponCommand toCommand() {
        return new CreateCouponCommand(
            code, type, value,
            currency == null || currency.isBlank() ? "BRL" : currency,
            validFrom, validUntil, minOrderValue, maxUses);
    }
}
