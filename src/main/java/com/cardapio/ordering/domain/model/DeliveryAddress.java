package com.cardapio.ordering.domain.model;

import java.util.Objects;
import java.util.UUID;

public record DeliveryAddress(
    String street,
    String number,
    String complement,
    String district,
    String city,
    String postalCode,
    UUID neighborhoodId
) {
    public DeliveryAddress {
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(number, "number");
        Objects.requireNonNull(district, "district");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(postalCode, "postalCode");
        Objects.requireNonNull(neighborhoodId, "neighborhoodId");
        if (street.isBlank()) throw new IllegalArgumentException("street must not be blank");
        if (number.isBlank()) throw new IllegalArgumentException("number must not be blank");
        if (district.isBlank()) throw new IllegalArgumentException("district must not be blank");
        if (city.isBlank()) throw new IllegalArgumentException("city must not be blank");
        if (postalCode.isBlank()) throw new IllegalArgumentException("postalCode must not be blank");
    }
}
