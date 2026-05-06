package com.cardapio.ordering.application.dto;

import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderView(
    UUID id,
    UUID customerId,
    OrderModality modality,
    OrderStatus status,
    List<OrderItemView> items,
    BigDecimal subtotal,
    BigDecimal deliveryFee,
    BigDecimal discount,
    BigDecimal total,
    String currency,
    DeliveryAddressView address,
    Instant placedAt,
    Instant updatedAt
) {
    public record DeliveryAddressView(
        String street, String number, String complement,
        String district, String city, String postalCode, UUID neighborhoodId
    ) {}
}
