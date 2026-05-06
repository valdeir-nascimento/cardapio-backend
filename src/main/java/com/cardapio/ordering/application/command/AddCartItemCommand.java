package com.cardapio.ordering.application.command;

import java.util.List;
import java.util.UUID;

public record AddCartItemCommand(
    UUID customerId,
    UUID productId,
    UUID variationId,
    List<AddOnSelection> addOns,
    UUID halfLeftProductId,
    UUID halfRightProductId,
    String observation,
    int quantity
) {
    public record AddOnSelection(UUID groupId, UUID itemId, int quantity) {}
}
