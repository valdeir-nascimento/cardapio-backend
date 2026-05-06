package com.cardapio.notification.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentStreamEvent(
    UUID paymentId,
    UUID orderId,
    String method,
    String status,
    BigDecimal amount
) {}
