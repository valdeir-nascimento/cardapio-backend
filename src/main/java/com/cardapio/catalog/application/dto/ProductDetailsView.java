// ProductDetailsView.java
package com.cardapio.catalog.application.dto;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.shared.domain.Money;

import java.util.List;

public record ProductDetailsView(
    ProductId id,
    CategoryId categoryId,
    String name,
    String description,
    Money basePrice,
    String imageUrl,
    boolean available,
    boolean allowsHalfHalf,
    Integer stockQuantity,        // null = untracked
    List<VariationView> variations,
    List<AddOnGroupView> addOnGroups
) {}
