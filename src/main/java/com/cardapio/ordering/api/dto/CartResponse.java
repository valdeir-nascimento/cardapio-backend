package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.CartItemView;
import com.cardapio.ordering.application.dto.CartView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
    UUID id,
    UUID customerId,
    List<CartItemResponse> items,
    BigDecimal subtotal,
    String currency,
    boolean hasUnavailableItems
) {
    public static CartResponse from(CartView v) {
        List<CartItemResponse> items = v.items().stream().map(CartResponse::toItem).toList();
        return new CartResponse(v.id(), v.customerId(), items, v.subtotal(), v.currency(), v.hasUnavailableItems());
    }

    private static CartItemResponse toItem(CartItemView i) {
        return new CartItemResponse(i.id(), i.productId(), i.productName(), i.variationName(),
            i.addOnNames(), i.halfDescription(), i.observation(), i.quantity(), i.lineTotal(), i.available());
    }

    public record CartItemResponse(
        UUID id, UUID productId, String productName, String variationName,
        List<String> addOnNames, String halfDescription, String observation,
        int quantity, BigDecimal lineTotal, boolean available
    ) {}
}
