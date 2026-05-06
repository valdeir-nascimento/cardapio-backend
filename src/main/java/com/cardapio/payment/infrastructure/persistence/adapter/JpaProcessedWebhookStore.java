package com.cardapio.payment.infrastructure.persistence.adapter;

import com.cardapio.payment.domain.port.ProcessedWebhookStore;
import com.cardapio.payment.infrastructure.persistence.jpa.ProcessedWebhookJpaEntity;
import com.cardapio.payment.infrastructure.persistence.repository.SpringProcessedWebhookJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JpaProcessedWebhookStore implements ProcessedWebhookStore {

    private final SpringProcessedWebhookJpaRepository repo;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean registerIfAbsent(String payloadHash, Instant receivedAt) {
        if (repo.existsById(payloadHash)) return false;
        try {
            repo.save(new ProcessedWebhookJpaEntity(payloadHash, receivedAt));
            return true;
        } catch (DataIntegrityViolationException e) {
            // Another concurrent webhook registered the same hash
            return false;
        }
    }
}
