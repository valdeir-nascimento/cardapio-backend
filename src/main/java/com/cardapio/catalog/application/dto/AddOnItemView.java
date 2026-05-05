// AddOnItemView.java
package com.cardapio.catalog.application.dto;

import com.cardapio.catalog.domain.model.AddOnItemId;
import com.cardapio.shared.domain.Money;

public record AddOnItemView(AddOnItemId id, String name, Money price) {}
