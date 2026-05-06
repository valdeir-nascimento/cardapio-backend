package com.cardapio.delivery.domain.exception;

import com.cardapio.delivery.domain.model.NeighborhoodId;
import com.cardapio.shared.domain.DomainException;

public class NeighborhoodNotFoundException extends DomainException {
    public NeighborhoodNotFoundException(NeighborhoodId id) {
        super("NEIGHBORHOOD_NOT_FOUND", "neighborhood not found: " + id.value());
    }
}
