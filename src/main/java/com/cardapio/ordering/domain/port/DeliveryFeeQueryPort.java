package com.cardapio.ordering.domain.port;

import com.cardapio.shared.domain.Money;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryFeeQueryPort {
    Optional<Money> getActiveFee(UUID neighborhoodId);
}
