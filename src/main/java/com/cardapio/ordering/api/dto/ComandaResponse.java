package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.ComandaSummaryView;
import com.cardapio.ordering.application.dto.ComandaView;
import com.cardapio.ordering.domain.model.ComandaStatus;
import com.cardapio.ordering.domain.model.OrderId;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ComandaResponse(
    UUID id,
    UUID tableId,
    ComandaStatus status,
    Set<UUID> customerIds,
    List<UUID> orderIds,
    Instant openedAt,
    Instant closedAt
) {
    public static ComandaResponse from(ComandaView v) {
        return new ComandaResponse(
            v.id().value(),
            v.tableId().value(),
            v.status(),
            v.customerIds(),
            v.orderIds().stream().map(OrderId::value).toList(),
            v.openedAt(),
            v.closedAt());
    }

    public static ComandaResponse from(ComandaSummaryView v) {
        return new ComandaResponse(
            v.id().value(),
            v.tableId().value(),
            v.status(),
            Set.of(),
            List.of(),
            v.openedAt(),
            v.closedAt());
    }
}
