package com.cardapio.ordering.domain.exception;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.shared.domain.DomainException;

public class OrderNotFoundException extends DomainException {
    public OrderNotFoundException(OrderId id) {
        super("ORDER_NOT_FOUND", "order not found: " + id.value());
    }
}
