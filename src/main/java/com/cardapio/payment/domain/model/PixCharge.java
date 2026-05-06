package com.cardapio.payment.domain.model;

import java.time.Instant;
import java.util.Objects;

public record PixCharge(
    String gatewayTxId,
    String qrCode,
    String qrCodeBase64,
    Instant expiresAt
) {
    public PixCharge {
        Objects.requireNonNull(gatewayTxId, "gatewayTxId");
        Objects.requireNonNull(qrCode, "qrCode");
        Objects.requireNonNull(qrCodeBase64, "qrCodeBase64");
    }
}
