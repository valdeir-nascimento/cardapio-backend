package com.cardapio.delivery.application.command;

import java.math.BigDecimal;

public record CreateNeighborhoodCommand(String name, String city, BigDecimal fee) {
}
