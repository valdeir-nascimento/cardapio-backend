package com.cardapio.ordering.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartItemView(
    UUID id,
    UUID productId,
    String productName,
    String variationName,
    List<String> addOnNames,
    String halfDescription,
    String observation,
    int quantity,
    BigDecimal lineTotal,
    boolean available
) {}
