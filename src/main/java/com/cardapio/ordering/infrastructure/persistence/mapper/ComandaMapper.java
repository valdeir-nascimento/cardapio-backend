package com.cardapio.ordering.infrastructure.persistence.mapper;

import com.cardapio.ordering.domain.model.Comanda;
import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.ComandaStatus;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.TableId;
import com.cardapio.ordering.infrastructure.persistence.jpa.ComandaJpaEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ComandaMapper {

    private ComandaMapper() {}

    public static ComandaJpaEntity toJpa(Comanda comanda) {
        Set<UUID> customers = new java.util.LinkedHashSet<>(comanda.customerIds());
        List<UUID> orders = comanda.orderIds().stream().map(OrderId::value).toList();
        return new ComandaJpaEntity(
            comanda.id().value(),
            comanda.tableId().value(),
            comanda.status().name(),
            comanda.openedAt(),
            comanda.closedAt(),
            comanda.updatedAt(),
            customers,
            orders
        );
    }

    public static void update(ComandaJpaEntity entity, Comanda comanda) {
        entity.setStatus(comanda.status().name());
        entity.setClosedAt(comanda.closedAt());
        entity.setUpdatedAt(comanda.updatedAt());
        entity.setCustomerIds(new java.util.LinkedHashSet<>(comanda.customerIds()));
        entity.setOrderIds(comanda.orderIds().stream().map(OrderId::value).toList());
    }

    public static Comanda toDomain(ComandaJpaEntity e) {
        List<OrderId> orderIds = e.getOrderIds().stream().map(OrderId::of).toList();
        return Comanda.rehydrate(
            ComandaId.of(e.getId()),
            TableId.of(e.getTableId()),
            new java.util.LinkedHashSet<>(e.getCustomerIds()),
            orderIds,
            ComandaStatus.valueOf(e.getStatus()),
            e.getOpenedAt(),
            e.getClosedAt(),
            e.getUpdatedAt()
        );
    }
}
