package com.cardapio.ordering.domain.exception;

import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.shared.domain.DomainException;

public class IllegalStatusTransitionException extends DomainException {
    public IllegalStatusTransitionException(OrderStatus from, OrderStatus to, OrderModality modality) {
        super("ORDER_INVALID_TRANSITION",
            "transição inválida (" + modality + "): " + from + " → " + to);
    }
}
