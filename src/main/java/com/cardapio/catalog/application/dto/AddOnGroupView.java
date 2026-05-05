// AddOnGroupView.java
package com.cardapio.catalog.application.dto;

import com.cardapio.catalog.domain.model.AddOnGroupId;

import java.util.List;

public record AddOnGroupView(AddOnGroupId id, String name, int minSelection, int maxSelection, List<AddOnItemView> items) {}
