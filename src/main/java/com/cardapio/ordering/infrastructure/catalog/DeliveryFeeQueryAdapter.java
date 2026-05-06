package com.cardapio.ordering.infrastructure.catalog;

import com.cardapio.delivery.application.DeliveryFacade;
import com.cardapio.delivery.domain.model.NeighborhoodId;
import com.cardapio.ordering.domain.port.DeliveryFeeQueryPort;
import com.cardapio.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeliveryFeeQueryAdapter implements DeliveryFeeQueryPort {

    private final DeliveryFacade delivery;

    @Override
    public Optional<Money> getActiveFee(UUID neighborhoodId) {
        return delivery.getActiveFee(NeighborhoodId.of(neighborhoodId));
    }
}
