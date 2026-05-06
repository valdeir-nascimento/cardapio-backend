package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.OrderItemView;
import com.cardapio.ordering.application.dto.OrderView;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID customerId,
    OrderModality modality,
    OrderStatus status,
    List<OrderItemResponse> items,
    BigDecimal subtotal,
    BigDecimal deliveryFee,
    BigDecimal discount,
    BigDecimal total,
    String currency,
    AddressResponse address,
    Instant placedAt,
    Instant updatedAt
) {
    public static OrderResponse from(OrderView v) {
        AddressResponse addr = v.address() == null ? null
            : new AddressResponse(v.address().street(), v.address().number(), v.address().complement(),
                v.address().district(), v.address().city(), v.address().postalCode(), v.address().neighborhoodId());
        return new OrderResponse(v.id(), v.customerId(), v.modality(), v.status(),
            v.items().stream().map(OrderResponse::toItem).toList(),
            v.subtotal(), v.deliveryFee(), v.discount(), v.total(), v.currency(),
            addr, v.placedAt(), v.updatedAt());
    }

    private static OrderItemResponse toItem(OrderItemView i) {
        return new OrderItemResponse(i.id(), i.productId(), i.productName(), i.variationName(),
            i.addOnNames(), i.halfDescription(), i.observation(), i.quantity(), i.lineTotal());
    }

    public record OrderItemResponse(
        UUID id, UUID productId, String productName, String variationName,
        List<String> addOnNames, String halfDescription, String observation,
        int quantity, BigDecimal lineTotal
    ) {}

    public record AddressResponse(
        String street, String number, String complement,
        String district, String city, String postalCode, UUID neighborhoodId
    ) {}
}
