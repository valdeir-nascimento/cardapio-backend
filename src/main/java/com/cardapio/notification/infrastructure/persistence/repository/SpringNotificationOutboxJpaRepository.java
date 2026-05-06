package com.cardapio.notification.infrastructure.persistence.repository;

import com.cardapio.notification.infrastructure.persistence.jpa.NotificationOutboxJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringNotificationOutboxJpaRepository extends JpaRepository<NotificationOutboxJpaEntity, UUID> {

    @Query("""
        select e from NotificationOutboxJpaEntity e
        where e.status in ('PENDING', 'FAILED')
          and e.scheduledFor <= :now
        order by e.scheduledFor asc
    """)
    List<NotificationOutboxJpaEntity> findDueForDispatch(@Param("now") Instant now, Pageable pageable);
}
