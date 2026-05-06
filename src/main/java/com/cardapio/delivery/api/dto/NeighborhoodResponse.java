package com.cardapio.delivery.api.dto;

import com.cardapio.delivery.application.dto.NeighborhoodView;

import java.math.BigDecimal;
import java.util.UUID;

public record NeighborhoodResponse(UUID id, String name, String city, BigDecimal fee, String currency, boolean active) {
    public static NeighborhoodResponse from(NeighborhoodView v) {
        return new NeighborhoodResponse(v.id(), v.name(), v.city(), v.fee(), v.currency(), v.active());
    }
}
