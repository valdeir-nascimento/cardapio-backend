package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.command.OpenComandaCommand;
import com.cardapio.ordering.application.dto.ComandaView;
import com.cardapio.ordering.domain.model.Comanda;
import com.cardapio.ordering.domain.model.Table;
import com.cardapio.ordering.domain.port.ComandaRepository;
import com.cardapio.ordering.domain.port.TableRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class OpenComandaUseCase {

    private final TableRepository tables;
    private final ComandaRepository comandas;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public Result<ComandaView> execute(OpenComandaCommand cmd) {
        Table table = tables.findById(cmd.tableId()).orElse(null);
        if (table == null) return Result.failWith(ErrorCode.TABLE_NOT_FOUND);
        if (!table.isActive()) return Result.failWith(ErrorCode.TABLE_INACTIVE);

        if (comandas.findOpenByTableId(table.id()).isPresent()) {
            return Result.failWith(ErrorCode.COMANDA_ALREADY_OPEN);
        }

        Comanda comanda = Comanda.open(table.id(), cmd.openerCustomerId(), clock);
        comandas.save(comanda);
        comanda.pullDomainEvents().forEach(events::publishEvent);

        return Result.success(toView(comanda));
    }

    static ComandaView toView(Comanda c) {
        return new ComandaView(c.id(), c.tableId(), c.status(),
            c.customerIds(), c.orderIds(), c.openedAt(), c.closedAt());
    }
}
