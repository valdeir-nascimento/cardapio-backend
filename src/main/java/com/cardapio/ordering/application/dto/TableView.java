package com.cardapio.ordering.application.dto;

import com.cardapio.ordering.domain.model.TableId;

import java.util.UUID;

public record TableView(
    TableId id,
    int number,
    UUID qrToken,
    boolean active,
    boolean hasOpenComanda
) {}
