// CreateProductCommand.java
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
    List<VariationDraft> variations,
    List<AddOnGroupDraft> addOnGroups
) {
    public record VariationDraft(String name, Money priceModifier) {}
    public record AddOnGroupDraft(String name, int minSelection, int maxSelection, List<AddOnItemDraft> items) {}
    public record AddOnItemDraft(String name, Money price) {}
}
