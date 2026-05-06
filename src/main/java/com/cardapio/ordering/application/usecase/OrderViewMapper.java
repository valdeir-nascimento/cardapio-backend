package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.dto.OrderItemView;
import com.cardapio.ordering.application.dto.OrderSummaryView;
import com.cardapio.ordering.application.dto.OrderView;
import com.cardapio.ordering.domain.model.Order;
import com.cardapio.ordering.domain.model.OrderItem;
import com.cardapio.ordering.domain.model.SelectedAddOn;
import com.cardapio.ordering.domain.model.SelectedVariation;

import java.util.List;

public final class OrderViewMapper {

    private OrderViewMapper() {}

    public static OrderView toView(Order o) {
        List<OrderItemView> items = o.items().stream().map(OrderViewMapper::toItem).toList();
        OrderView.DeliveryAddressView address = o.address()
            .map(a -> new OrderView.DeliveryAddressView(
                a.street(), a.number(), a.complement(),
                a.district(), a.city(), a.postalCode(), a.neighborhoodId()))
            .orElse(null);

        return new OrderView(
            o.id().value(), o.customerId(), o.modality(), o.status(),
            items,
            o.subtotal().amount(), o.deliveryFee().amount(), o.discount().amount(), o.total().amount(),
            o.subtotal().currency().getCurrencyCode(),
            address,
            o.placedAt(), o.updatedAt()
        );
    }

    public static OrderSummaryView toSummary(Order o) {
        return new OrderSummaryView(
            o.id().value(), o.customerId(), o.modality(), o.status(),
            o.total().amount(), o.total().currency().getCurrencyCode(), o.placedAt()
        );
    }

    private static OrderItemView toItem(OrderItem oi) {
        return new OrderItemView(
            oi.id().value(), oi.productId(), oi.productName(),
            oi.variation().map(SelectedVariation::name).orElse(null),
            oi.addOns().stream().map(SelectedAddOn::name).toList(),
            oi.halfAndHalf().map(h -> h.displayName()).orElse(null),
            oi.observation().value(), oi.quantity(), oi.lineTotal().amount()
        );
    }
}
