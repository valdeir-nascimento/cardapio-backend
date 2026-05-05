package com.cardapio.catalog.api.dto;

import com.cardapio.catalog.application.dto.ProductDetailsView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductDetailsResponse(
    UUID id, UUID categoryId, String name, String description, BigDecimal basePrice,
    String imageUrl, boolean available, boolean allowsHalfHalf, Integer stockQuantity,
    List<VariationDto> variations, List<AddOnGroupDto> addOnGroups
) {
    public record VariationDto(UUID id, String name, BigDecimal priceModifier) {}
    public record AddOnGroupDto(UUID id, String name, int minSelection, int maxSelection, List<AddOnItemDto> items) {}
    public record AddOnItemDto(UUID id, String name, BigDecimal price) {}

    public static ProductDetailsResponse from(ProductDetailsView v) {
        return new ProductDetailsResponse(
            v.id().value(), v.categoryId().value(), v.name(), v.description(), v.basePrice().amount(),
            v.imageUrl(), v.available(), v.allowsHalfHalf(), v.stockQuantity(),
            v.variations().stream().map(va -> new VariationDto(va.id().value(), va.name(), va.priceModifier().amount())).toList(),
            v.addOnGroups().stream().map(g -> new AddOnGroupDto(g.id().value(), g.name(), g.minSelection(), g.maxSelection(),
                g.items().stream().map(i -> new AddOnItemDto(i.id().value(), i.name(), i.price().amount())).toList()))
                .toList());
    }
}
