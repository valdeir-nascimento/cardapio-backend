package com.cardapio.notification.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderStreamEvent(
    UUID orderId,
    String shortRef,
    String modality,
    String status,
    String previousStatus,
    BigDecimal total
) {
    public static OrderStreamEvent placed(UUID orderId, String shortRef, String modality, BigDecimal total) {
        return new OrderStreamEvent(orderId, shortRef, modality, "RECEIVED", null, total);
    }

    public static OrderStreamEvent statusChanged(UUID orderId, String shortRef, String previous, String current) {
        return new OrderStreamEvent(orderId, shortRef, null, current, previous, null);
    }

    public static OrderStreamEvent canceled(UUID orderId, String shortRef) {
        return new OrderStreamEvent(orderId, shortRef, null, "CANCELED", null, null);
    }
}
