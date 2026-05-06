package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.command.AdvanceOrderStatusCommand;
import com.cardapio.ordering.domain.event.OrderStatusChanged;
import com.cardapio.ordering.domain.exception.IllegalStatusTransitionException;
import com.cardapio.ordering.domain.model.Order;
import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.ordering.domain.port.OrderRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class AdvanceOrderStatusUseCase {

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public Result<Void> execute(AdvanceOrderStatusCommand cmd) {
        Order order = orders.findById(cmd.orderId()).orElse(null);
        if (order == null) return Result.failWith(ErrorCode.ORDER_NOT_FOUND);

        OrderStatus prev = order.status();
        try {
            order.advance(cmd.target(), clock);
        } catch (IllegalStatusTransitionException e) {
            return Result.failWith(ErrorCode.ORDER_INVALID_TRANSITION, e.getMessage());
        }
        orders.save(order);
        events.publishEvent(OrderStatusChanged.of(order.id(), prev, order.status(), order.updatedAt()));
        return Result.ok();
    }
}
