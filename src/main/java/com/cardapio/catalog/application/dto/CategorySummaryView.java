package com.cardapio.catalog.application.dto;

import com.cardapio.catalog.domain.model.CategoryId;

public record CategorySummaryView(CategoryId id, String name, int displayOrder, boolean active) {}
