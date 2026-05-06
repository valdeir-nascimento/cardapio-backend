package com.cardapio.payment.domain.port;

import com.cardapio.payment.domain.model.PaymentTransaction;
import com.cardapio.payment.domain.model.PaymentTransactionId;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository {
    void save(PaymentTransaction tx);
    Optional<PaymentTransaction> findById(PaymentTransactionId id);
    Optional<PaymentTransaction> findActiveByOrderId(UUID orderId);
    Optional<PaymentTransaction> findByGatewayTxId(String gatewayTxId);
}
