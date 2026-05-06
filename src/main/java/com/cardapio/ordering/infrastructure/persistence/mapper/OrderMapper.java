package com.cardapio.ordering.infrastructure.persistence.mapper;

import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.DeliveryAddress;
import com.cardapio.ordering.domain.model.HalfAndHalf;
import com.cardapio.ordering.domain.model.Observation;
import com.cardapio.ordering.domain.model.Order;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderItem;
import com.cardapio.ordering.domain.model.OrderItemId;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.ordering.domain.model.SelectedAddOn;
import com.cardapio.ordering.domain.model.SelectedVariation;
import com.cardapio.ordering.domain.model.TableId;
import com.cardapio.ordering.infrastructure.persistence.jpa.OrderItemAddOnJpaEntity;
import com.cardapio.ordering.infrastructure.persistence.jpa.OrderItemJpaEntity;
import com.cardapio.ordering.infrastructure.persistence.jpa.OrderJpaEntity;
import com.cardapio.shared.domain.Money;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class OrderMapper {

    private OrderMapper() {}

    public static OrderJpaEntity toJpa(Order order) {
        Optional<DeliveryAddress> address = order.address();
        OrderJpaEntity entity = new OrderJpaEntity(
            order.id().value(),
            order.customerId(),
            order.modality().name(),
            order.status().name(),
            order.subtotal().amount(),
            order.deliveryFee().amount(),
            order.discount().amount(),
            order.total().amount(),
            order.subtotal().currency().getCurrencyCode(),
            address.map(DeliveryAddress::street).orElse(null),
            address.map(DeliveryAddress::number).orElse(null),
            address.map(DeliveryAddress::complement).orElse(null),
            address.map(DeliveryAddress::district).orElse(null),
            address.map(DeliveryAddress::city).orElse(null),
            address.map(DeliveryAddress::postalCode).orElse(null),
            address.map(DeliveryAddress::neighborhoodId).orElse(null),
            order.tableId().map(TableId::value).orElse(null),
            order.comandaId().map(ComandaId::value).orElse(null),
            order.placedAt(),
            order.updatedAt()
        );
        int pos = 0;
        for (OrderItem oi : order.items()) {
            entity.getItems().add(toItemJpa(oi, order.id().value(), order.subtotal().currency().getCurrencyCode(), pos++));
        }
        return entity;
    }

    public static void updateStatus(OrderJpaEntity entity, Order order) {
        entity.setStatus(order.status().name());
        entity.setUpdatedAt(order.updatedAt());
    }

    private static OrderItemJpaEntity toItemJpa(OrderItem oi, java.util.UUID orderId, String currency, int position) {
        UUID variationId = oi.variation().map(SelectedVariation::variationId).orElse(null);
        String variationName = oi.variation().map(SelectedVariation::name).orElse(null);
        var variationModifier = oi.variation().map(v -> v.priceModifier().amount()).orElse(null);

        UUID halfLeft = oi.halfAndHalf().map(HalfAndHalf::leftProductId).orElse(null);
        UUID halfRight = oi.halfAndHalf().map(HalfAndHalf::rightProductId).orElse(null);
        String halfName = oi.halfAndHalf().map(HalfAndHalf::displayName).orElse(null);
        var halfBase = oi.halfAndHalf().map(h -> h.basePrice().amount()).orElse(null);

        OrderItemJpaEntity entity = new OrderItemJpaEntity(
            oi.id().value(), orderId, oi.productId(), oi.productName(),
            variationId, variationName, variationModifier,
            halfLeft, halfRight, halfName, halfBase,
            oi.observation().value(), oi.quantity(), oi.lineTotal().amount(), currency, position
        );
        int aPos = 0;
        for (SelectedAddOn ao : oi.addOns()) {
            entity.getAddOns().add(new OrderItemAddOnJpaEntity(
                UUID.randomUUID(), oi.id().value(),
                ao.groupId(), ao.itemId(), ao.name(),
                ao.price().amount(), ao.price().currency().getCurrencyCode(),
                ao.quantity(), aPos++
            ));
        }
        return entity;
    }

    public static Order toDomain(OrderJpaEntity e) {
        Currency currency = Currency.getInstance(e.getCurrency());
        Optional<DeliveryAddress> address = e.getAddressStreet() == null
            ? Optional.empty()
            : Optional.of(new DeliveryAddress(
                e.getAddressStreet(), e.getAddressNumber(), e.getAddressComplement(),
                e.getAddressDistrict(), e.getAddressCity(), e.getAddressPostalCode(),
                e.getAddressNeighborhoodId()));

        List<OrderItem> items = e.getItems().stream().map(OrderMapper::toDomainItem).toList();

        Optional<TableId> tableId = e.getTableId() == null ? Optional.empty() : Optional.of(TableId.of(e.getTableId()));
        Optional<ComandaId> comandaId = e.getComandaId() == null ? Optional.empty() : Optional.of(ComandaId.of(e.getComandaId()));

        return Order.rehydrate(
            OrderId.of(e.getId()),
            e.getCustomerId(),
            OrderModality.valueOf(e.getModality()),
            OrderStatus.valueOf(e.getStatus()),
            items,
            Money.of(e.getSubtotal(), currency),
            Money.of(e.getDeliveryFee(), currency),
            Money.of(e.getDiscount(), currency),
            Money.of(e.getTotal(), currency),
            address,
            tableId,
            comandaId,
            e.getPlacedAt(),
            e.getUpdatedAt()
        );
    }

    private static OrderItem toDomainItem(OrderItemJpaEntity e) {
        Currency currency = Currency.getInstance(e.getCurrency());

        Optional<SelectedVariation> variation = e.getVariationId() == null
            ? Optional.empty()
            : Optional.of(new SelectedVariation(
                e.getVariationId(), e.getVariationName(),
                Money.of(e.getVariationModifier(), currency)));

        Optional<HalfAndHalf> halfAndHalf = e.getHalfLeftProductId() == null
            ? Optional.empty()
            : Optional.of(new HalfAndHalf(
                e.getHalfLeftProductId(), e.getHalfRightProductId(),
                e.getHalfDisplayName(),
                Money.of(e.getHalfBasePrice(), currency)));

        List<SelectedAddOn> addOns = e.getAddOns().stream()
            .map(a -> new SelectedAddOn(
                a.getAddOnGroupId(), a.getAddOnItemId(), a.getName(),
                Money.of(a.getPrice(), Currency.getInstance(a.getCurrency())),
                a.getQuantity()))
            .toList();

        return OrderItem.rehydrate(
            OrderItemId.of(e.getId()),
            e.getProductId(),
            e.getProductName(),
            variation,
            addOns,
            halfAndHalf,
            new Observation(e.getObservation()),
            e.getQuantity(),
            Money.of(e.getLineTotal(), currency)
        );
    }
}
