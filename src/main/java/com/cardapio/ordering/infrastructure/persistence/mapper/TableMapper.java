package com.cardapio.ordering.infrastructure.persistence.mapper;

import com.cardapio.ordering.domain.model.Table;
import com.cardapio.ordering.domain.model.TableId;
import com.cardapio.ordering.infrastructure.persistence.jpa.TableJpaEntity;

public final class TableMapper {

    private TableMapper() {}

    public static TableJpaEntity toJpa(Table table) {
        return new TableJpaEntity(
            table.id().value(),
            table.number(),
            table.qrToken(),
            table.qrImageKey(),
            table.isActive(),
            table.createdAt(),
            table.updatedAt()
        );
    }

    public static void update(TableJpaEntity entity, Table table) {
        entity.setQrImageKey(table.qrImageKey());
        entity.setActive(table.isActive());
        entity.setUpdatedAt(table.updatedAt());
    }

    public static Table toDomain(TableJpaEntity e) {
        return Table.rehydrate(
            TableId.of(e.getId()),
            e.getNumber(),
            e.getQrToken(),
            e.getQrImageKey(),
            Boolean.TRUE.equals(e.getActive()),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
