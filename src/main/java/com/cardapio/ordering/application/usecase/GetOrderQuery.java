package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.dto.OrderView;
import com.cardapio.ordering.domain.model.Order;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.port.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetOrderQuery {

    private final OrderRepository orders;

    @Transactional(readOnly = true)
    public Optional<OrderView> getForCustomer(OrderId id, UUID customerId) {
        return orders.findById(id)
            .filter(o -> o.customerId().equals(customerId))
            .map(OrderViewMapper::toView);
    }

    @Transactional(readOnly = true)
    public Optional<OrderView> getAdmin(OrderId id) {
        return orders.findById(id).map(OrderViewMapper::toView);
    }

    @Transactional(readOnly = true)
    public Optional<Order> findRaw(OrderId id) {
        return orders.findById(id);
    }
}
