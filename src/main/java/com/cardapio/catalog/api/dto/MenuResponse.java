package com.cardapio.catalog.api.dto;

import com.cardapio.catalog.application.dto.MenuView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MenuResponse(List<CategoryDto> categories) {

    public record CategoryDto(UUID id, String name, int displayOrder, List<ProductDto> products) {}
    public record ProductDto(UUID id, String name, String description, BigDecimal basePrice, String imageUrl) {}

    public static MenuResponse from(MenuView v) {
        var cats = v.categories().stream()
            .map(c -> new CategoryDto(c.id().value(), c.name(), c.displayOrder(),
                c.products().stream()
                    .map(p -> new ProductDto(p.id().value(), p.name(), p.description(), p.basePrice().amount(), p.imageUrl()))
                    .toList()))
            .toList();
        return new MenuResponse(cats);
    }
}
