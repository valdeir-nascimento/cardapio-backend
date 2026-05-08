package com.cardapio.catalog.api.dto;

import com.cardapio.catalog.application.dto.ProductDetailsView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Detalhes completos de um produto: variações, adicionais e estoque.")
public record ProductDetailsResponse(

    @Schema(example = "ad4f3c1b-7a02-4c41-bb9c-d56b9c92ec0a", format = "uuid")
    UUID id,

    @Schema(example = "9c5f50d2-7a02-4c41-bb9c-d56b9c92ec0a", format = "uuid")
    UUID categoryId,

    @Schema(example = "Pizza Margherita")
    String name,

    @Schema(example = "Molho de tomate San Marzano, mussarela de búfala, manjericão fresco e azeite extravirgem.")
    String description,

    @Schema(description = "Preço base em reais.", example = "49.90")
    BigDecimal basePrice,

    @Schema(example = "https://cdn.cardapio.example.com/products/margherita.jpg", nullable = true)
    String imageUrl,

    @Schema(description = "Se está disponível para pedido agora (toggle do admin).", example = "true")
    boolean available,

    @Schema(description = "Se aceita meia-meia (típico de pizza).", example = "true")
    boolean allowsHalfHalf,

    @Schema(description = "Estoque atual. `null` = não controlado.", example = "20", nullable = true)
    Integer stockQuantity,

    List<VariationDto> variations,

    List<AddOnGroupDto> addOnGroups
) {

    @Schema(description = "Variação de tamanho/formato disponível.")
    public record VariationDto(
        @Schema(example = "5fa6f72a-3b8e-4922-a6cb-882c0a32b2e6", format = "uuid") UUID id,
        @Schema(example = "Tamanho Grande (12 fatias)") String name,
        @Schema(description = "Diferença em reais aplicada ao preço base.", example = "15.00")
        BigDecimal priceModifier
    ) {}

    @Schema(description = "Grupo de adicionais opcionais.")
    public record AddOnGroupDto(
        @Schema(format = "uuid") UUID id,
        @Schema(example = "Bordas Recheadas") String name,
        @Schema(description = "Mínimo obrigatório (0 = opcional).", example = "0") int minSelection,
        @Schema(description = "Máximo permitido.", example = "1") int maxSelection,
        List<AddOnItemDto> items
    ) {}

    @Schema(description = "Item dentro de um grupo de adicionais.")
    public record AddOnItemDto(
        @Schema(format = "uuid") UUID id,
        @Schema(example = "Borda de Catupiry") String name,
        @Schema(description = "Preço extra em reais.", example = "8.50") BigDecimal price
    ) {}

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
