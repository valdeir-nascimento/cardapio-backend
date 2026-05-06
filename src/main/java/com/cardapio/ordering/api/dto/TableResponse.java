package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.TableView;

import java.util.UUID;

public record TableResponse(
    UUID id,
    int number,
    UUID qrToken,
    boolean active,
    boolean hasOpenComanda
) {
    public static TableResponse from(TableView v) {
        return new TableResponse(v.id().value(), v.number(), v.qrToken(), v.active(), v.hasOpenComanda());
    }
}
