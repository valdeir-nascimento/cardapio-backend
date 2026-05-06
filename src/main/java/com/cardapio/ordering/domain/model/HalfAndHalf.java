package com.cardapio.ordering.domain.model;

import com.cardapio.shared.domain.Money;

import java.util.Objects;
import java.util.UUID;

public record HalfAndHalf(UUID leftProductId, UUID rightProductId, String displayName, Money basePrice) {
    public HalfAndHalf {
        Objects.requireNonNull(leftProductId, "leftProductId");
        Objects.requireNonNull(rightProductId, "rightProductId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(basePrice, "basePrice");
        if (leftProductId.equals(rightProductId)) {
            throw new IllegalArgumentException("left and right products must differ");
        }
        if (displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");
    }
}
