package com.cardapio.delivery.application.command;

import com.cardapio.delivery.domain.model.NeighborhoodId;

import java.math.BigDecimal;

public record UpdateNeighborhoodCommand(NeighborhoodId id, String name, String city, BigDecimal fee, boolean active) {
}
