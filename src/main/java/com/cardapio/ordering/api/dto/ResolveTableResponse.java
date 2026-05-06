package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.ResolveTableView;
import com.cardapio.ordering.domain.model.ComandaId;

import java.util.UUID;

public record ResolveTableResponse(UUID tableId, int number, UUID currentComandaId) {
    public static ResolveTableResponse from(ResolveTableView v) {
        return new ResolveTableResponse(
            v.tableId().value(),
            v.number(),
            v.currentComandaId().map(ComandaId::value).orElse(null));
    }
}
