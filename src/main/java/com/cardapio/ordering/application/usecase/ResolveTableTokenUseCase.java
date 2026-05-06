package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.dto.ResolveTableView;
import com.cardapio.ordering.domain.model.Comanda;
import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.Table;
import com.cardapio.ordering.domain.port.ComandaRepository;
import com.cardapio.ordering.domain.port.TableRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResolveTableTokenUseCase {

    private final TableRepository tables;
    private final ComandaRepository comandas;

    @Transactional(readOnly = true)
    public Result<ResolveTableView> execute(UUID token) {
        Table table = tables.findByQrToken(token).orElse(null);
        if (table == null) return Result.failWith(ErrorCode.TABLE_NOT_FOUND);
        if (!table.isActive()) return Result.failWith(ErrorCode.TABLE_INACTIVE);

        Optional<ComandaId> currentComandaId = comandas.findOpenByTableId(table.id())
            .map(Comanda::id);

        return Result.success(new ResolveTableView(table.id(), table.number(), currentComandaId));
    }
}
