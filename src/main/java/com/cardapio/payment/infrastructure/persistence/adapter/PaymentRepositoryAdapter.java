package com.cardapio.payment.infrastructure.persistence.adapter;

import com.cardapio.payment.domain.model.PaymentStatus;
import com.cardapio.payment.domain.model.PaymentTransaction;
import com.cardapio.payment.domain.model.PaymentTransactionId;
import com.cardapio.payment.domain.port.PaymentTransactionRepository;
import com.cardapio.payment.infrastructure.persistence.mapper.PaymentMapper;
import com.cardapio.payment.infrastructure.persistence.repository.SpringPaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentTransactionRepository {

    private final SpringPaymentJpaRepository jpa;

    @Override
    public void save(PaymentTransaction tx) {
        var existing = jpa.findById(tx.id().value());
        if (existing.isPresent()) {
            PaymentMapper.update(existing.get(), tx);
            jpa.save(existing.get());
        } else {
            jpa.save(PaymentMapper.toJpa(tx));
        }
    }

    @Override
    public Optional<PaymentTransaction> findById(PaymentTransactionId id) {
        return jpa.findById(id.value()).map(PaymentMapper::toDomain);
    }

    @Override
    public Optional<PaymentTransaction> findActiveByOrderId(UUID orderId) {
        return jpa.findByOrderIdAndStatus(orderId, PaymentStatus.PENDING.name())
            .map(PaymentMapper::toDomain);
    }

    @Override
    public Optional<PaymentTransaction> findByGatewayTxId(String gatewayTxId) {
        return jpa.findByGatewayTxId(gatewayTxId).map(PaymentMapper::toDomain);
    }
}
