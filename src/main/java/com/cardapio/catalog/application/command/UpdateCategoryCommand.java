// UpdateCategoryCommand.java
package com.cardapio.catalog.application.command;

import com.cardapio.catalog.domain.model.CategoryId;
public record UpdateCategoryCommand(CategoryId id, String name, int displayOrder, boolean active) {}
