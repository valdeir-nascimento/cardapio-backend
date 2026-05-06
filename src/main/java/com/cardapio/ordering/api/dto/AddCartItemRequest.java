package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.command.AddCartItemCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AddCartItemRequest(
    @NotNull UUID productId,
    UUID variationId,
    List<AddOnSelectionRequest> addOns,
    UUID halfLeftProductId,
    UUID halfRightProductId,
    @Size(max = 200) String observation,
    @Min(1) int quantity
) {
    public AddCartItemCommand toCommand(UUID customerId) {
        List<AddCartItemCommand.AddOnSelection> sel = addOns == null ? List.of()
            : addOns.stream().map(a -> new AddCartItemCommand.AddOnSelection(a.groupId(), a.itemId(), a.quantity())).toList();
        return new AddCartItemCommand(customerId, productId, variationId, sel,
            halfLeftProductId, halfRightProductId, observation, quantity);
    }

    public record AddOnSelectionRequest(@NotNull UUID groupId, @NotNull UUID itemId, @Min(1) int quantity) {}
}
