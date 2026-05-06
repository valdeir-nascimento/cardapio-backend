package com.cardapio.delivery.application.dto;

import com.cardapio.delivery.domain.model.Neighborhood;

import java.math.BigDecimal;
import java.util.UUID;

public record NeighborhoodView(UUID id, String name, String city, BigDecimal fee, String currency, boolean active) {
    public static NeighborhoodView from(Neighborhood n) {
        return new NeighborhoodView(
            n.id().value(),
            n.name(),
            n.city(),
            n.fee().amount(),
            n.fee().currency().getCurrencyCode(),
            n.isActive()
        );
    }
}
