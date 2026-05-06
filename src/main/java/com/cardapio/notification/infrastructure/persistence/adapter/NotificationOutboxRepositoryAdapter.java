package com.cardapio.notification.infrastructure.persistence.adapter;

import com.cardapio.notification.domain.model.NotificationOutbox;
import com.cardapio.notification.domain.model.NotificationOutboxId;
import com.cardapio.notification.domain.port.NotificationOutboxRepository;
import com.cardapio.notification.infrastructure.persistence.mapper.NotificationOutboxMapper;
import com.cardapio.notification.infrastructure.persistence.repository.SpringNotificationOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationOutboxRepositoryAdapter implements NotificationOutboxRepository {

    private final SpringNotificationOutboxJpaRepository jpa;

    @Override
    public void save(NotificationOutbox outbox) {
        var existing = jpa.findById(outbox.id().value());
        if (existing.isPresent()) {
            NotificationOutboxMapper.update(existing.get(), outbox);
            jpa.save(existing.get());
        } else {
            jpa.save(NotificationOutboxMapper.toJpa(outbox));
        }
    }

    @Override
    public Optional<NotificationOutbox> findById(NotificationOutboxId id) {
        return jpa.findById(id.value()).map(NotificationOutboxMapper::toDomain);
    }

    @Override
    public List<NotificationOutbox> findDueForDispatch(Instant now, int limit) {
        return jpa.findDueForDispatch(now, PageRequest.of(0, limit))
            .stream()
            .map(NotificationOutboxMapper::toDomain)
            .toList();
    }
}
