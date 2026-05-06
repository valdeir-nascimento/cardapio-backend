package com.cardapio.ordering.application.dto;

import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.ComandaStatus;
import com.cardapio.ordering.domain.model.TableId;

import java.time.Instant;

public record ComandaSummaryView(
    ComandaId id,
    TableId tableId,
    ComandaStatus status,
    int customers,
    int orders,
    Instant openedAt,
    Instant closedAt
) {}
