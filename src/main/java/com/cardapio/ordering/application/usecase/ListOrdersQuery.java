package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.dto.OrderSummaryView;
import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.ordering.domain.port.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListOrdersQuery {

    private final OrderRepository orders;

    @Transactional(readOnly = true)
    public List<OrderSummaryView> listMyOrders(UUID customerId, int limit, int offset) {
        return orders.findByCustomerId(customerId, limit, offset).stream()
            .map(OrderViewMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryView> listAdmin(OrderStatus status, Instant from, Instant to, int limit, int offset) {
        return orders.findAdmin(status, from, to, limit, offset).stream()
            .map(OrderViewMapper::toSummary).toList();
    }
}
