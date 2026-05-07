package com.cardapio.review.infrastructure.persistence.repository;

import com.cardapio.review.infrastructure.persistence.jpa.ReviewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringReviewJpaRepository extends JpaRepository<ReviewJpaEntity, UUID> {
    Optional<ReviewJpaEntity> findByOrderIdAndCustomerId(UUID orderId, UUID customerId);
    boolean existsByOrderIdAndCustomerId(UUID orderId, UUID customerId);
}
