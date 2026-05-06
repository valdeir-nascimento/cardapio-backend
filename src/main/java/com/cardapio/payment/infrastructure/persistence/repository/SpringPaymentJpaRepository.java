package com.cardapio.payment.infrastructure.persistence.repository;

import com.cardapio.payment.infrastructure.persistence.jpa.PaymentTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringPaymentJpaRepository extends JpaRepository<PaymentTransactionJpaEntity, UUID> {

    Optional<PaymentTransactionJpaEntity> findByOrderIdAndStatus(UUID orderId, String status);

    Optional<PaymentTransactionJpaEntity> findByGatewayTxId(String gatewayTxId);
}
