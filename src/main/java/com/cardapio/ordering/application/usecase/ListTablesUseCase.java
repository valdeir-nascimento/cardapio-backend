package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.dto.TableView;
import com.cardapio.ordering.domain.model.Comanda;
import com.cardapio.ordering.domain.model.ComandaStatus;
import com.cardapio.ordering.domain.model.TableId;
import com.cardapio.ordering.domain.port.ComandaRepository;
import com.cardapio.ordering.domain.port.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListTablesUseCase {

    private final TableRepository tables;
    private final ComandaRepository comandas;

    @Transactional(readOnly = true)
    public List<TableView> execute() {
        Set<TableId> openTableIds = comandas.findByStatus(ComandaStatus.OPEN).stream()
            .map(Comanda::tableId)
            .collect(Collectors.toUnmodifiableSet());

        return tables.findAllOrderByNumber().stream()
            .map(t -> new TableView(t.id(), t.number(), t.qrToken(), t.isActive(), openTableIds.contains(t.id())))
            .toList();
    }
}
