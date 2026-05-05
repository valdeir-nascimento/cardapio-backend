// VariationView.java
package com.cardapio.catalog.application.dto;

import com.cardapio.catalog.domain.model.VariationId;
import com.cardapio.shared.domain.Money;

public record VariationView(VariationId id, String name, Money priceModifier) {}
