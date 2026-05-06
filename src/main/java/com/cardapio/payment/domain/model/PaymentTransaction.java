package com.cardapio.payment.domain.model;

import com.cardapio.shared.domain.AggregateRoot;
import com.cardapio.shared.domain.Money;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PaymentTransaction extends AggregateRoot<PaymentTransactionId> {

    private final UUID orderId;
    private final UUID customerId;
    private final PaymentMethod method;
    private final Money amount;
    private PaymentStatus status;
    private String gatewayTxId;
    private String qrCode;
    private String qrCodeBase64;
    private String cardBrand;
    private String cardLast4;
    private String failureReason;
    private final List<WebhookEventLog> events;
    private final Instant createdAt;
    private Instant updatedAt;

    private PaymentTransaction(PaymentTransactionId id, UUID orderId, UUID customerId,
                               PaymentMethod method, Money amount, PaymentStatus status,
                               String gatewayTxId, String qrCode, String qrCodeBase64,
                               String cardBrand, String cardLast4, String failureReason,
                               List<WebhookEventLog> events,
                               Instant createdAt, Instant updatedAt) {
        super(id);
        this.orderId = Objects.requireNonNull(orderId, "orderId");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.method = Objects.requireNonNull(method, "method");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.status = Objects.requireNonNull(status, "status");
        this.gatewayTxId = gatewayTxId;
        this.qrCode = qrCode;
        this.qrCodeBase64 = qrCodeBase64;
        this.cardBrand = cardBrand;
        this.cardLast4 = cardLast4;
        this.failureReason = failureReason;
        this.events = new ArrayList<>(Objects.requireNonNull(events, "events"));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static PaymentTransaction initiate(UUID orderId, UUID customerId,
                                              PaymentMethod method, Money amount, Clock clock) {
        Instant now = clock.instant();
        return new PaymentTransaction(
            PaymentTransactionId.newId(), orderId, customerId, method, amount,
            PaymentStatus.PENDING, null, null, null, null, null, null,
            new ArrayList<>(), now, now);
    }

    public static PaymentTransaction rehydrate(PaymentTransactionId id, UUID orderId, UUID customerId,
                                               PaymentMethod method, Money amount, PaymentStatus status,
                                               String gatewayTxId, String qrCode, String qrCodeBase64,
                                               String cardBrand, String cardLast4, String failureReason,
                                               List<WebhookEventLog> events,
                                               Instant createdAt, Instant updatedAt) {
        return new PaymentTransaction(id, orderId, customerId, method, amount, status,
            gatewayTxId, qrCode, qrCodeBase64, cardBrand, cardLast4, failureReason,
            events, createdAt, updatedAt);
    }

    public void attachPixCharge(PixCharge pix, Clock clock) {
        requireStatus(PaymentStatus.PENDING);
        if (method != PaymentMethod.PIX) throw new IllegalStateException("not a Pix transaction");
        this.gatewayTxId = pix.gatewayTxId();
        this.qrCode = pix.qrCode();
        this.qrCodeBase64 = pix.qrCodeBase64();
        this.updatedAt = clock.instant();
    }

    public void attachCardCharge(CardCharge card, Clock clock) {
        requireStatus(PaymentStatus.PENDING);
        if (method != PaymentMethod.CARD) throw new IllegalStateException("not a Card transaction");
        this.gatewayTxId = card.gatewayTxId();
        this.cardBrand = card.brand();
        this.cardLast4 = card.last4();
        this.status = card.status();
        this.updatedAt = clock.instant();
    }

    public void approve(Clock clock) {
        if (status == PaymentStatus.APPROVED) return;
        if (status.isTerminal()) {
            throw new IllegalStateException("cannot approve from terminal " + status);
        }
        this.status = PaymentStatus.APPROVED;
        this.updatedAt = clock.instant();
    }

    public void reject(Clock clock) {
        if (status == PaymentStatus.REJECTED) return;
        if (status.isTerminal()) {
            throw new IllegalStateException("cannot reject from terminal " + status);
        }
        this.status = PaymentStatus.REJECTED;
        this.updatedAt = clock.instant();
    }

    public void markRefunded(Clock clock) {
        if (status != PaymentStatus.APPROVED) {
            throw new IllegalStateException("can only refund APPROVED, was " + status);
        }
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = clock.instant();
    }

    public void markFailed(Clock clock, String reason) {
        if (status.isTerminal()) {
            throw new IllegalStateException("already terminal: " + status);
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = clock.instant();
    }

    public void recordWebhookEvent(WebhookEventLog log) {
        this.events.add(Objects.requireNonNull(log, "log"));
    }

    private void requireStatus(PaymentStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("expected status " + expected + " but was " + status);
        }
    }

    public UUID orderId() { return orderId; }
    public UUID customerId() { return customerId; }
    public PaymentMethod method() { return method; }
    public Money amount() { return amount; }
    public PaymentStatus status() { return status; }
    public String gatewayTxId() { return gatewayTxId; }
    public String qrCode() { return qrCode; }
    public String qrCodeBase64() { return qrCodeBase64; }
    public String cardBrand() { return cardBrand; }
    public String cardLast4() { return cardLast4; }
    public String failureReason() { return failureReason; }
    public List<WebhookEventLog> events() { return List.copyOf(events); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
