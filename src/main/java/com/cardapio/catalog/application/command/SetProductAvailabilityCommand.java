// SetProductAvailabilityCommand.java
package com.cardapio.catalog.application.command;

import com.cardapio.catalog.domain.model.ProductId;
public record SetProductAvailabilityCommand(ProductId id, boolean available) {}
