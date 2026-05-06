package com.cardapio.payment.infrastructure.ordering;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.ordering.application.OrderingFacade;
import com.cardapio.ordering.application.dto.OrderView;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.payment.domain.port.OrderQueryPort;
import com.cardapio.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class OrderQueryAdapter implements OrderQueryPort {

    private final OrderingFacade ordering;

    @Override
    public Optional<OrderSnapshot> loadOrder(UUID orderId) {
        try {
            OrderView view = ordering.getOrderAdmin(OrderId.of(orderId));
            return Optional.of(new OrderSnapshot(
                view.id(),
                view.customerId(),
                Money.of(view.total(), Currency.getInstance(view.currency())),
                view.status(),
                view.modality()
            ));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }
}
