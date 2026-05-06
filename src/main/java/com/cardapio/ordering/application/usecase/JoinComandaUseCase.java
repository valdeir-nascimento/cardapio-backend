package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.command.JoinComandaCommand;
import com.cardapio.ordering.application.dto.ComandaView;
import com.cardapio.ordering.domain.exception.DineInInvariantException;
import com.cardapio.ordering.domain.model.Comanda;
import com.cardapio.ordering.domain.port.ComandaRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class JoinComandaUseCase {

    private final ComandaRepository comandas;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public Result<ComandaView> execute(JoinComandaCommand cmd) {
        Comanda comanda = comandas.findById(cmd.comandaId()).orElse(null);
        if (comanda == null) return Result.failWith(ErrorCode.COMANDA_NOT_FOUND);
        try {
            comanda.join(cmd.customerId(), clock);
        } catch (DineInInvariantException e) {
            return Result.failWith(ErrorCode.COMANDA_CLOSED, e.getMessage());
        }
        comandas.save(comanda);
        comanda.pullDomainEvents().forEach(events::publishEvent);

        return Result.success(new ComandaView(comanda.id(), comanda.tableId(), comanda.status(),
            comanda.customerIds(), comanda.orderIds(), comanda.openedAt(), comanda.closedAt()));
    }
}
