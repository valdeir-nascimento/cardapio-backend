package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.command.PlaceOrderCommand;
import com.cardapio.ordering.domain.model.OrderModality;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PlaceOrderRequest(
    @NotNull OrderModality modality,
    @Valid AddressRequest address
) {
    public PlaceOrderCommand toCommand(UUID customerId, String idempotencyKey) {
        PlaceOrderCommand.DeliveryAddressInput a = address == null ? null
            : new PlaceOrderCommand.DeliveryAddressInput(
                address.street(), address.number(), address.complement(),
                address.district(), address.city(), address.postalCode(), address.neighborhoodId());
        return new PlaceOrderCommand(customerId, modality, a, idempotencyKey);
    }

    public record AddressRequest(
        @NotBlank @Size(max = 160) String street,
        @NotBlank @Size(max = 20) String number,
        @Size(max = 120) String complement,
        @NotBlank @Size(max = 120) String district,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 16) String postalCode,
        @NotNull UUID neighborhoodId
    ) {}
}
