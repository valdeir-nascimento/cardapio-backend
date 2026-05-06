package com.cardapio.ordering.application.command;

import java.util.UUID;

public record RemoveCartItemCommand(UUID customerId, UUID cartItemId) {
}
