package com.cardapio.notification.infrastructure.ordering;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.notification.domain.port.OrderSummaryPort;
import com.cardapio.ordering.application.OrderingFacade;
import com.cardapio.ordering.application.dto.OrderView;
import com.cardapio.ordering.domain.model.OrderId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class OrderSummaryAdapter implements OrderSummaryPort {

    private final OrderingFacade orderingFacade;

    @Override
    public Optional<OrderSummary> find(UUID orderId) {
        try {
            OrderView v = orderingFacade.getOrderAdmin(OrderId.of(orderId));
            return Optional.of(new OrderSummary(
                v.id(),
                v.customerId(),
                shortRef(v.id()),
                v.modality(),
                v.status(),
                v.total()
            ));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    static String shortRef(UUID id) {
        return id.toString().replace("-", "").substring(0, 6).toUpperCase();
    }
}
