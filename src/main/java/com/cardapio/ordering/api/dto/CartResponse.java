package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.dto.CartItemView;
import com.cardapio.ordering.application.dto.CartView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Carrinho atual do cliente: itens, subtotal e cupom aplicado.")
public record CartResponse(

    @Schema(description = "ID do carrinho.", example = "11ec51c8-b4ee-4cd3-9a51-9d3fa1c39e3d", format = "uuid")
    UUID id,

    @Schema(description = "Cliente dono do carrinho.",
        example = "c2b41216-14f8-4d2c-a0d8-34b97dc7b897", format = "uuid")
    UUID customerId,

    List<CartItemResponse> items,

    @Schema(description = "Soma das linhas (sem desconto).", example = "98.40")
    BigDecimal subtotal,

    @Schema(description = "Moeda ISO-4217.", example = "BRL")
    String currency,

    @Schema(description = "Algum item ficou indisponível depois de adicionado (ex: produto desativado).",
        example = "false")
    boolean hasUnavailableItems,

    @Schema(description = "Cupom aplicado, se houver.", example = "BEMVINDO10", nullable = true)
    String couponCode,

    @Schema(description = "Valor descontado pelo cupom.", example = "9.84", nullable = true)
    BigDecimal discount,

    @Schema(description = "Total após desconto = subtotal - discount.", example = "88.56")
    BigDecimal discountedTotal
) {
    public static CartResponse from(CartView v) {
        List<CartItemResponse> items = v.items().stream().map(CartResponse::toItem).toList();
        return new CartResponse(v.id(), v.customerId(), items, v.subtotal(), v.currency(),
            v.hasUnavailableItems(), v.couponCode(), v.discount(), v.discountedTotal());
    }

    private static CartItemResponse toItem(CartItemView i) {
        return new CartItemResponse(i.id(), i.productId(), i.productName(), i.variationName(),
            i.addOnNames(), i.halfDescription(), i.observation(), i.quantity(), i.lineTotal(), i.available());
    }

    @Schema(description = "Linha do carrinho com produto, variação, adicionais e preço calculado.")
    public record CartItemResponse(
        @Schema(format = "uuid") UUID id,
        @Schema(format = "uuid") UUID productId,
        @Schema(example = "Pizza Margherita") String productName,
        @Schema(example = "Tamanho Grande", nullable = true) String variationName,
        @Schema(description = "Nomes dos adicionais selecionados.", example = "[\"Borda de Catupiry\"]")
        List<String> addOnNames,
        @Schema(description = "Descrição da meia-meia, se aplicável.",
            example = "1/2 Margherita + 1/2 Calabresa", nullable = true)
        String halfDescription,
        @Schema(example = "Sem cebola.", nullable = true) String observation,
        @Schema(example = "1") int quantity,
        @Schema(description = "Preço total desta linha (preço unitário × quantity).", example = "57.40")
        BigDecimal lineTotal,
        @Schema(description = "Se o produto continua disponível.", example = "true") boolean available
    ) {}
}
