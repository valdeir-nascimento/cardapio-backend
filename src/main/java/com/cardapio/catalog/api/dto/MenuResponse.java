package com.cardapio.catalog.api.dto;

import com.cardapio.catalog.application.dto.MenuView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Menu público completo, agrupado por categoria. Apenas categorias e produtos ativos são incluídos.")
public record MenuResponse(

    @Schema(description = "Lista de categorias ordenadas por `displayOrder`.")
    List<CategoryDto> categories
) {

    @Schema(description = "Categoria com seus produtos ativos.")
    public record CategoryDto(

        @Schema(example = "9c5f50d2-7a02-4c41-bb9c-d56b9c92ec0a", format = "uuid")
        UUID id,

        @Schema(example = "Pizzas Tradicionais")
        String name,

        @Schema(example = "10")
        int displayOrder,

        List<ProductDto> products
    ) {}

    @Schema(description = "Resumo de produto exibido no menu (sem variações/adicionais — para detalhes use `GET /api/v1/menu/products/{id}`).")
    public record ProductDto(

        @Schema(example = "ad4f3c1b-7a02-4c41-bb9c-d56b9c92ec0a", format = "uuid")
        UUID id,

        @Schema(example = "Pizza Margherita")
        String name,

        @Schema(example = "Molho de tomate San Marzano, mussarela de búfala, manjericão fresco e azeite extravirgem.")
        String description,

        @Schema(description = "Preço base em reais.", example = "49.90")
        BigDecimal basePrice,

        @Schema(example = "https://cdn.cardapio.example.com/products/margherita.jpg", nullable = true)
        String imageUrl
    ) {}

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
