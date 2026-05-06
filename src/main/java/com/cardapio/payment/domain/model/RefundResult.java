package com.cardapio.payment.domain.model;

import java.time.Instant;
import java.util.Objects;

public record RefundResult(String gatewayRefundId, Instant refundedAt) {
    public RefundResult {
        Objects.requireNonNull(gatewayRefundId, "gatewayRefundId");
        Objects.requireNonNull(refundedAt, "refundedAt");
    }
}
