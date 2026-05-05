// ProductSummaryView.java
package com.cardapio.catalog.application.dto;

import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.shared.domain.Money;

public record ProductSummaryView(ProductId id, String name, String description, Money basePrice, String imageUrl) {}
