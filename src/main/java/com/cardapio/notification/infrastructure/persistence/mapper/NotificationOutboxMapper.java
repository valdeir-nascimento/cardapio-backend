package com.cardapio.notification.infrastructure.persistence.mapper;

import com.cardapio.notification.domain.model.NotificationChannel;
import com.cardapio.notification.domain.model.NotificationOutbox;
import com.cardapio.notification.domain.model.NotificationOutboxId;
import com.cardapio.notification.domain.model.NotificationStatus;
import com.cardapio.notification.domain.model.NotificationTemplate;
import com.cardapio.notification.infrastructure.persistence.jpa.NotificationOutboxJpaEntity;

public final class NotificationOutboxMapper {

    private NotificationOutboxMapper() {}

    public static NotificationOutboxJpaEntity toJpa(NotificationOutbox o) {
        return new NotificationOutboxJpaEntity(
            o.id().value(),
            o.channel().name(),
            o.template().name(),
            o.recipientId(),
            o.payload(),
            o.status().name(),
            o.attempts(),
            o.lastError(),
            o.scheduledFor(),
            o.createdAt(),
            o.updatedAt()
        );
    }

    public static void update(NotificationOutboxJpaEntity entity, NotificationOutbox o) {
        entity.setStatus(o.status().name());
        entity.setAttempts(o.attempts());
        entity.setLastError(o.lastError());
        entity.setScheduledFor(o.scheduledFor());
        entity.setUpdatedAt(o.updatedAt());
    }

    public static NotificationOutbox toDomain(NotificationOutboxJpaEntity e) {
        return NotificationOutbox.rehydrate(
            NotificationOutboxId.of(e.getId()),
            NotificationChannel.valueOf(e.getChannel()),
            NotificationTemplate.valueOf(e.getTemplate()),
            e.getRecipientId(),
            e.getPayload(),
            NotificationStatus.valueOf(e.getStatus()),
            e.getAttempts(),
            e.getLastError(),
            e.getScheduledFor(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
