package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.OrderItemView;
import com.cardapio.ordering.application.dto.OrderView;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Pedido completo com itens, endereço e totais detalhados.")
public record OrderResponse(

    @Schema(format = "uuid", example = "21a98176-44df-49eb-a37c-b8329f1e0123") UUID id,
    @Schema(format = "uuid", example = "c2b41216-14f8-4d2c-a0d8-34b97dc7b897") UUID customerId,
    @Schema(example = "DELIVERY") OrderModality modality,
    @Schema(description = "Estado atual no fluxo do pedido.", example = "PREPARING")
    OrderStatus status,
    List<OrderItemResponse> items,
    @Schema(description = "Soma das linhas.", example = "88.40") BigDecimal subtotal,
    @Schema(description = "Taxa de entrega (zero quando não é delivery).", example = "10.00") BigDecimal deliveryFee,
    @Schema(description = "Desconto de cupom.", example = "0.00") BigDecimal discount,
    @Schema(description = "Total final cobrado.", example = "98.40") BigDecimal total,
    @Schema(example = "BRL") String currency,
    @Schema(nullable = true) AddressResponse address,
    @Schema(format = "date-time", example = "2026-05-07T20:30:00Z") Instant placedAt,
    @Schema(format = "date-time", example = "2026-05-07T20:32:14Z") Instant updatedAt
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

    @Schema(description = "Linha do pedido — espelha o estado do carrinho no momento do checkout.")
    public record OrderItemResponse(
        @Schema(format = "uuid") UUID id,
        @Schema(format = "uuid") UUID productId,
        @Schema(example = "Pizza Margherita") String productName,
        @Schema(nullable = true, example = "Tamanho Grande") String variationName,
        List<String> addOnNames,
        @Schema(nullable = true) String halfDescription,
        @Schema(nullable = true) String observation,
        @Schema(example = "1") int quantity,
        @Schema(example = "57.40") BigDecimal lineTotal
    ) {}

    @Schema(description = "Endereço de entrega congelado no momento do pedido.")
    public record AddressResponse(
        @Schema(example = "Rua das Flores") String street,
        @Schema(example = "123") String number,
        @Schema(nullable = true, example = "Apto 42") String complement,
        @Schema(example = "Vila Mariana") String district,
        @Schema(example = "São Paulo") String city,
        @Schema(example = "04101-300") String postalCode,
        @Schema(format = "uuid") UUID neighborhoodId
    ) {}
}
