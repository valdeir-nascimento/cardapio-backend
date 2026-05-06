package com.cardapio.payment.infrastructure.persistence.repository;

import com.cardapio.payment.infrastructure.persistence.jpa.ProcessedWebhookJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringProcessedWebhookJpaRepository extends JpaRepository<ProcessedWebhookJpaEntity, String> {
}
