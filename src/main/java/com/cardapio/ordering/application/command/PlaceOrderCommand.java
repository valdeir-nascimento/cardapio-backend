package com.cardapio.ordering.application.command;

import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.TableId;

import java.util.UUID;

public record PlaceOrderCommand(
    UUID customerId,
    OrderModality modality,
    DeliveryAddressInput address,
    TableId tableId,
    ComandaId comandaId,
    String idempotencyKey
) {
    public PlaceOrderCommand(UUID customerId, OrderModality modality,
                             DeliveryAddressInput address, String idempotencyKey) {
        this(customerId, modality, address, null, null, idempotencyKey);
    }

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
