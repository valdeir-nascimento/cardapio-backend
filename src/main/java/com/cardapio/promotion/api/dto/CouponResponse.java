package com.cardapio.promotion.api.dto;

import com.cardapio.promotion.application.dto.CouponView;
import com.cardapio.promotion.domain.model.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Cupom de desconto com contadores de uso.")
public record CouponResponse(

    @Schema(format = "uuid", example = "fab12345-cccc-4321-9876-abcdef012345") UUID id,
    @Schema(example = "BEMVINDO10") String code,
    @Schema(example = "PERCENTAGE") DiscountType type,
    @Schema(example = "10.00") BigDecimal value,
    @Schema(example = "BRL") String currency,
    @Schema(format = "date-time", example = "2026-05-01T00:00:00Z", nullable = true) Instant validFrom,
    @Schema(format = "date-time", example = "2026-12-31T23:59:59Z", nullable = true) Instant validUntil,
    @Schema(example = "50.00", nullable = true) BigDecimal minOrderValue,
    @Schema(description = "Limite total. null = ilimitado.", example = "100", nullable = true) Integer maxUses,
    @Schema(description = "Quantas vezes já foi usado.", example = "37") int usesCount,
    @Schema(description = "Se está ativo (não expirado, não desativado, não esgotado).", example = "true") boolean active,
    @Schema(format = "date-time", example = "2026-04-15T10:00:00Z") Instant createdAt,
    @Schema(format = "date-time", example = "2026-05-07T18:00:00Z") Instant updatedAt
) {
    public static CouponResponse from(CouponView v) {
        return new CouponResponse(
            v.id().value(), v.code(), v.type(), v.value(), v.currency(),
            v.validFrom(), v.validUntil(), v.minOrderValue(), v.maxUses(),
            v.usesCount(), v.active(), v.createdAt(), v.updatedAt());
    }
}
