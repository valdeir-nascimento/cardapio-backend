package com.cardapio.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Dados para criar ou atualizar um produto do menu.")
public record ProductRequest(

    @Schema(description = "Nome do produto.", example = "Pizza Margherita",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank String name,

    @Schema(description = "Descrição detalhada (ingredientes, modo de preparo).",
        example = "Molho de tomate San Marzano, mussarela de búfala, manjericão fresco e azeite extravirgem.",
        nullable = true)
    String description,

    @Schema(description = "Preço base em reais (BRL). Variações somam o `priceModifier`.",
        example = "49.90", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0.01")
    @NotNull @Positive BigDecimal basePrice,

    @Schema(description = "ID da categoria à qual o produto pertence.",
        example = "9c5f50d2-7a02-4c41-bb9c-d56b9c92ec0a", format = "uuid",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull UUID categoryId,

    @Schema(description = "URL pública da imagem do produto.",
        example = "https://cdn.cardapio.example.com/products/margherita.jpg",
        nullable = true)
    String imageUrl,

    @Schema(description = "Se o produto pode ser pedido como meia/meia (típico de pizzas).",
        example = "true")
    boolean allowsHalfHalf,

    @Schema(description = "Variações de tamanho/formato. Vazio se o produto tem apenas um formato.",
        nullable = true)
    @Valid List<VariationRequest> variations,

    @Schema(description = "Grupos de adicionais opcionais. Vazio se não há adicionais.",
        nullable = true)
    @Valid List<AddOnGroupRequest> addOnGroups
) {}
