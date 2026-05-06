package com.cardapio.ordering.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartView(
    UUID id,
    UUID customerId,
    List<CartItemView> items,
    BigDecimal subtotal,
    String currency,
    boolean hasUnavailableItems,
    String couponCode,
    BigDecimal discount,
    BigDecimal discountedTotal
) {}
