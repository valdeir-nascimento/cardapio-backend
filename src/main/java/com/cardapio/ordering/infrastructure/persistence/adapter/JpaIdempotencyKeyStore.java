package com.cardapio.ordering.infrastructure.persistence.adapter;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.port.IdempotencyKeyStore;
import com.cardapio.ordering.infrastructure.persistence.jpa.IdempotencyKeyJpaEntity;
import com.cardapio.ordering.infrastructure.persistence.jpa.IdempotencyKeyJpaEntity.IdempotencyKeyId;
import com.cardapio.ordering.infrastructure.persistence.repository.SpringIdempotencyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaIdempotencyKeyStore implements IdempotencyKeyStore {

    private final SpringIdempotencyJpaRepository repo;
    private final Clock clock;

    @Override
    public Optional<OrderId> findExisting(String key, UUID customerId) {
        return repo.findById(new IdempotencyKeyId(customerId, key))
            .map(e -> OrderId.of(e.getOrderId()));
    }

    @Override
    public void register(String key, UUID customerId, OrderId orderId) {
        repo.save(new IdempotencyKeyJpaEntity(
            new IdempotencyKeyId(customerId, key),
            orderId.value(),
            clock.instant()
        ));
    }
}
