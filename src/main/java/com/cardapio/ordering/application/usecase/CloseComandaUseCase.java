package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.command.CloseComandaCommand;
import com.cardapio.ordering.application.dto.ComandaSummaryView;
import com.cardapio.ordering.domain.model.Comanda;
import com.cardapio.ordering.domain.model.ComandaStatus;
import com.cardapio.ordering.domain.model.Order;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.ordering.domain.port.ComandaRepository;
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
public class CloseComandaUseCase {

    private final ComandaRepository comandas;
    private final OrderRepository orders;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public Result<ComandaSummaryView> execute(CloseComandaCommand cmd) {
        Comanda comanda = comandas.findById(cmd.comandaId()).orElse(null);
        if (comanda == null) return Result.failWith(ErrorCode.COMANDA_NOT_FOUND);
        if (comanda.status() == ComandaStatus.CLOSED) return Result.failWith(ErrorCode.COMANDA_CLOSED);

        for (OrderId orderId : comanda.orderIds()) {
            Order o = orders.findById(orderId).orElse(null);
            if (o == null) continue;
            OrderStatus s = o.status();
            if (s != OrderStatus.SERVED && s != OrderStatus.CANCELED) {
                return Result.failWith(ErrorCode.COMANDA_HAS_PENDING_ORDERS,
                    "order " + orderId.value() + " is in status " + s);
            }
        }

        comanda.close(clock);
        comandas.save(comanda);
        comanda.pullDomainEvents().forEach(events::publishEvent);

        return Result.success(toSummary(comanda));
    }

    static ComandaSummaryView toSummary(Comanda c) {
        return new ComandaSummaryView(
            c.id(), c.tableId(), c.status(),
            c.customerIds().size(), c.orderIds().size(),
            c.openedAt(), c.closedAt());
    }
}
