package com.cardapio.ordering.application.dto;

import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.ComandaStatus;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.TableId;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ComandaView(
    ComandaId id,
    TableId tableId,
    ComandaStatus status,
    Set<UUID> customerIds,
    List<OrderId> orderIds,
    Instant openedAt,
    Instant closedAt
) {}
