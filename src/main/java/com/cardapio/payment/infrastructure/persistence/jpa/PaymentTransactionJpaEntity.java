package com.cardapio.payment.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransactionJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(nullable = false, length = 8) private String method;
    @Column(nullable = false, length = 12) private String status;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "gateway_tx_id", length = 120) private String gatewayTxId;
    @Column(name = "qr_code", columnDefinition = "TEXT") private String qrCode;
    @Column(name = "qr_code_base64", columnDefinition = "TEXT") private String qrCodeBase64;
    @Column(name = "card_brand", length = 40) private String cardBrand;
    @Column(name = "card_last4", length = 4) private String cardLast4;
    @Column(name = "failure_reason", length = 500) private String failureReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @OneToMany(mappedBy = "paymentTxId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<PaymentWebhookEventJpaEntity> events = new ArrayList<>();

    protected PaymentTransactionJpaEntity() {}

    public PaymentTransactionJpaEntity(UUID id, UUID orderId, UUID customerId, String method, String status,
                                       BigDecimal amount, String currency,
                                       String gatewayTxId, String qrCode, String qrCodeBase64,
                                       String cardBrand, String cardLast4, String failureReason,
                                       Instant createdAt, Instant updatedAt) {
        this.id = id; this.orderId = orderId; this.customerId = customerId;
        this.method = method; this.status = status;
        this.amount = amount; this.currency = currency;
        this.gatewayTxId = gatewayTxId; this.qrCode = qrCode; this.qrCodeBase64 = qrCodeBase64;
        this.cardBrand = cardBrand; this.cardLast4 = cardLast4; this.failureReason = failureReason;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getCustomerId() { return customerId; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getGatewayTxId() { return gatewayTxId; }
    public String getQrCode() { return qrCode; }
    public String getQrCodeBase64() { return qrCodeBase64; }
    public String getCardBrand() { return cardBrand; }
    public String getCardLast4() { return cardLast4; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<PaymentWebhookEventJpaEntity> getEvents() { return events; }

    public void setStatus(String status) { this.status = status; }
    public void setGatewayTxId(String gatewayTxId) { this.gatewayTxId = gatewayTxId; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public void setQrCodeBase64(String qrCodeBase64) { this.qrCodeBase64 = qrCodeBase64; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
