// SetProductStockCommand.java
package com.cardapio.catalog.application.command;

import com.cardapio.catalog.domain.model.ProductId;
public record SetProductStockCommand(ProductId id, Integer quantity) {  // null = untracked
}
