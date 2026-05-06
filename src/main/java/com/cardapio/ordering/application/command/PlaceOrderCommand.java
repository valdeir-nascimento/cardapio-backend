package com.cardapio.ordering.application.command;

import com.cardapio.ordering.domain.model.OrderModality;

import java.util.UUID;

public record PlaceOrderCommand(
    UUID customerId,
    OrderModality modality,
    DeliveryAddressInput address,
    String idempotencyKey
) {
    public record DeliveryAddressInput(
        String street,
        String number,
        String complement,
        String district,
        String city,
        String postalCode,
        UUID neighborhoodId
    ) {}
}
