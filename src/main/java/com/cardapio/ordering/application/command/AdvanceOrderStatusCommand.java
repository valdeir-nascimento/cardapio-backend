package com.cardapio.ordering.application.command;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderStatus;

public record AdvanceOrderStatusCommand(OrderId orderId, OrderStatus target) {
}
