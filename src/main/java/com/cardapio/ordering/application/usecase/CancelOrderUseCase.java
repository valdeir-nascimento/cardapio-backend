package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.command.CancelOrderCommand;
import com.cardapio.ordering.domain.event.OrderCanceled;
import com.cardapio.ordering.domain.exception.IllegalStatusTransitionException;
import com.cardapio.ordering.domain.model.Order;
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
public class CancelOrderUseCase {

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public Result<Void> execute(CancelOrderCommand cmd) {
        Order order = orders.findById(cmd.orderId()).orElse(null);
        if (order == null) return Result.failWith(ErrorCode.ORDER_NOT_FOUND);
        try {
            order.cancel(clock);
        } catch (IllegalStatusTransitionException e) {
            return Result.failWith(ErrorCode.ORDER_INVALID_TRANSITION, e.getMessage());
        }
        orders.save(order);
        events.publishEvent(OrderCanceled.of(order.id(), order.customerId(), order.updatedAt()));
        return Result.ok();
    }
}
