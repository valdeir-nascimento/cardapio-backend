package com.cardapio.payment.infrastructure.persistence.mapper;

import com.cardapio.payment.domain.model.PaymentMethod;
import com.cardapio.payment.domain.model.PaymentStatus;
import com.cardapio.payment.domain.model.PaymentTransaction;
import com.cardapio.payment.domain.model.PaymentTransactionId;
import com.cardapio.payment.domain.model.WebhookEventLog;
import com.cardapio.payment.infrastructure.persistence.jpa.PaymentTransactionJpaEntity;
import com.cardapio.payment.infrastructure.persistence.jpa.PaymentWebhookEventJpaEntity;
import com.cardapio.shared.domain.Money;

import java.util.Currency;
import java.util.List;

public final class PaymentMapper {

    private PaymentMapper() {}

    public static PaymentTransactionJpaEntity toJpa(PaymentTransaction tx) {
        PaymentTransactionJpaEntity entity = new PaymentTransactionJpaEntity(
            tx.id().value(), tx.orderId(), tx.customerId(),
            tx.method().name(), tx.status().name(),
            tx.amount().amount(), tx.amount().currency().getCurrencyCode(),
            tx.gatewayTxId(), tx.qrCode(), tx.qrCodeBase64(),
            tx.cardBrand(), tx.cardLast4(), tx.failureReason(),
            tx.createdAt(), tx.updatedAt()
        );
        int pos = 0;
        for (WebhookEventLog log : tx.events()) {
            entity.getEvents().add(new PaymentWebhookEventJpaEntity(
                log.id(), tx.id().value(), log.payloadHash(),
                log.rawPayload(), log.receivedAt(), pos++
            ));
        }
        return entity;
    }

    public static void update(PaymentTransactionJpaEntity entity, PaymentTransaction tx) {
        entity.setStatus(tx.status().name());
        entity.setGatewayTxId(tx.gatewayTxId());
        entity.setQrCode(tx.qrCode());
        entity.setQrCodeBase64(tx.qrCodeBase64());
        entity.setCardBrand(tx.cardBrand());
        entity.setCardLast4(tx.cardLast4());
        entity.setFailureReason(tx.failureReason());
        entity.setUpdatedAt(tx.updatedAt());
        // Append new webhook events (não-existentes pelo id)
        var existingIds = entity.getEvents().stream().map(PaymentWebhookEventJpaEntity::getId).toList();
        int pos = entity.getEvents().size();
        for (WebhookEventLog log : tx.events()) {
            if (!existingIds.contains(log.id())) {
                entity.getEvents().add(new PaymentWebhookEventJpaEntity(
                    log.id(), tx.id().value(), log.payloadHash(),
                    log.rawPayload(), log.receivedAt(), pos++
                ));
            }
        }
    }

    public static PaymentTransaction toDomain(PaymentTransactionJpaEntity e) {
        Currency currency = Currency.getInstance(e.getCurrency());
        List<WebhookEventLog> events = e.getEvents().stream()
            .map(ev -> new WebhookEventLog(ev.getId(), ev.getPayloadHash(), ev.getRawPayload(), ev.getReceivedAt()))
            .toList();
        return PaymentTransaction.rehydrate(
            PaymentTransactionId.of(e.getId()),
            e.getOrderId(), e.getCustomerId(),
            PaymentMethod.valueOf(e.getMethod()),
            Money.of(e.getAmount(), currency),
            PaymentStatus.valueOf(e.getStatus()),
            e.getGatewayTxId(), e.getQrCode(), e.getQrCodeBase64(),
            e.getCardBrand(), e.getCardLast4(), e.getFailureReason(),
            events,
            e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
