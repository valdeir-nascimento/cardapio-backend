package com.cardapio.ordering.application.dto;

import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.TableId;

import java.util.Optional;

public record ResolveTableView(
    TableId tableId,
    int number,
    Optional<ComandaId> currentComandaId
) {}
