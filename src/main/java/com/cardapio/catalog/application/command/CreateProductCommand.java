package com.cardapio.catalog.application.command;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.shared.domain.Money;

import java.util.List;

public record CreateProductCommand(
    String name,
    String description,
    Money basePrice,
    CategoryId categoryId,
    String imageUrl,
    boolean allowsHalfHalf,
    List<ProductDrafts.VariationDraft> variations,
    List<ProductDrafts.AddOnGroupDraft> addOnGroups
) {
    public CreateProductCommand {
        variations = variations != null ? List.copyOf(variations) : List.of();
        addOnGroups = addOnGroups != null ? List.copyOf(addOnGroups) : List.of();
    }
}
