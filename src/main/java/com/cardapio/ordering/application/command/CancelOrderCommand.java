package com.cardapio.ordering.application.command;

import com.cardapio.ordering.domain.model.OrderId;

public record CancelOrderCommand(OrderId orderId) {
}
