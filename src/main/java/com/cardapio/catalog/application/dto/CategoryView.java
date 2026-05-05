// CategoryView.java
package com.cardapio.catalog.application.dto;

import com.cardapio.catalog.domain.model.CategoryId;

import java.util.List;

public record CategoryView(CategoryId id, String name, int displayOrder, List<ProductSummaryView> products) {}
