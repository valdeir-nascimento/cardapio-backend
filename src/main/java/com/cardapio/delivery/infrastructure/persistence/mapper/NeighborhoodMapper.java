package com.cardapio.delivery.infrastructure.persistence.mapper;

import com.cardapio.delivery.domain.model.Neighborhood;
import com.cardapio.delivery.domain.model.NeighborhoodId;
import com.cardapio.delivery.infrastructure.persistence.jpa.NeighborhoodJpaEntity;
import com.cardapio.shared.domain.Money;

import java.time.Instant;
import java.util.Currency;

public final class NeighborhoodMapper {
    private NeighborhoodMapper() {}

    public static NeighborhoodJpaEntity toJpa(Neighborhood n, Instant now) {
        return new NeighborhoodJpaEntity(
            n.id().value(), n.name(), n.city(),
            n.fee().amount(), n.fee().currency().getCurrencyCode(),
            n.isActive(), now, now
        );
    }

    public static void updateJpa(NeighborhoodJpaEntity entity, Neighborhood n, Instant now) {
        entity.setName(n.name());
        entity.setCity(n.city());
        entity.setFee(n.fee().amount());
        entity.setCurrency(n.fee().currency().getCurrencyCode());
        entity.setActive(n.isActive());
        entity.setUpdatedAt(now);
    }

    public static Neighborhood toDomain(NeighborhoodJpaEntity e) {
        return Neighborhood.rehydrate(
            NeighborhoodId.of(e.getId()),
            e.getName(),
            e.getCity(),
            Money.of(e.getFee(), Currency.getInstance(e.getCurrency())),
            e.isActive()
        );
    }
}
