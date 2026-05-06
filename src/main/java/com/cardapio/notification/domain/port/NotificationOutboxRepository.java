package com.cardapio.notification.domain.port;

import com.cardapio.notification.domain.model.NotificationOutbox;
import com.cardapio.notification.domain.model.NotificationOutboxId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository {

    void save(NotificationOutbox outbox);

    Optional<NotificationOutbox> findById(NotificationOutboxId id);

    /**
     * Returns up to {@code limit} rows in PENDING/FAILED status whose
     * {@code scheduledFor} is at or before {@code now}, ordered by
     * scheduledFor ascending.
     */
    List<NotificationOutbox> findDueForDispatch(Instant now, int limit);
}
