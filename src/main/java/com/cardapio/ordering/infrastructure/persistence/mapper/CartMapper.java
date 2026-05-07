package com.cardapio.ordering.infrastructure.persistence.mapper;

import com.cardapio.ordering.domain.model.Cart;
import com.cardapio.ordering.domain.model.CartId;
import com.cardapio.ordering.domain.model.CartItem;
import com.cardapio.ordering.domain.model.CartItemId;
import com.cardapio.ordering.domain.model.HalfAndHalf;
import com.cardapio.ordering.domain.model.Observation;
import com.cardapio.ordering.domain.model.SelectedAddOn;
import com.cardapio.ordering.domain.model.SelectedVariation;
import com.cardapio.ordering.infrastructure.persistence.jpa.CartItemAddOnJpaEntity;
import com.cardapio.ordering.infrastructure.persistence.jpa.CartItemJpaEntity;
import com.cardapio.ordering.infrastructure.persistence.jpa.CartJpaEntity;
import com.cardapio.shared.domain.Money;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CartMapper {

    private static final String DEFAULT_CURRENCY = "BRL";

    private CartMapper() {}

    public static CartJpaEntity toJpa(Cart cart) {
        CartJpaEntity entity = new CartJpaEntity(
            cart.id().value(), cart.customerId(),
            cart.couponCode().orElse(null),
            cart.createdAt(), cart.updatedAt());
        int pos = 0;
        for (CartItem ci : cart.items()) {
            entity.getItems().add(toItemJpa(ci, cart.id().value(), pos++));
        }
        return entity;
    }

    public static void rebuildItems(CartJpaEntity entity, Cart cart) {
        entity.setUpdatedAt(cart.updatedAt());
        entity.setCouponCode(cart.couponCode().orElse(null));
        entity.getItems().clear();
        int pos = 0;
        for (CartItem ci : cart.items()) {
            entity.getItems().add(toItemJpa(ci, cart.id().value(), pos++));
        }
    }

    private static CartItemJpaEntity toItemJpa(CartItem ci, UUID cartId, int position) {
        UUID variationId = ci.variation().map(SelectedVariation::variationId).orElse(null);
        String variationName = ci.variation().map(SelectedVariation::name).orElse(null);
        var variationModifier = ci.variation().map(v -> v.priceModifier().amount()).orElse(null);

        UUID halfLeft = ci.halfAndHalf().map(HalfAndHalf::leftProductId).orElse(null);
        UUID halfRight = ci.halfAndHalf().map(HalfAndHalf::rightProductId).orElse(null);
        String halfName = ci.halfAndHalf().map(HalfAndHalf::displayName).orElse(null);
        var halfBase = ci.halfAndHalf().map(h -> h.basePrice().amount()).orElse(null);

        CartItemJpaEntity entity = new CartItemJpaEntity(
            ci.id().value(), cartId, ci.productId(),
            variationId, variationName, variationModifier,
            halfLeft, halfRight, halfName, halfBase,
            ci.observation().value(), ci.quantity(), position, DEFAULT_CURRENCY
        );

        int aPos = 0;
        for (SelectedAddOn ao : ci.addOns()) {
            entity.getAddOns().add(new CartItemAddOnJpaEntity(
                UUID.randomUUID(), ci.id().value(),
                ao.groupId(), ao.itemId(), ao.name(),
                ao.price().amount(), ao.price().currency().getCurrencyCode(),
                ao.quantity(), aPos++
            ));
        }
        return entity;
    }

    public static Cart toDomain(CartJpaEntity e) {
        List<CartItem> items = e.getItems().stream().map(CartMapper::toDomainItem).toList();
        return Cart.rehydrate(CartId.of(e.getId()), e.getCustomerId(), items,
            e.getCouponCode(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static CartItem toDomainItem(CartItemJpaEntity e) {
        Optional<SelectedVariation> variation = e.getVariationId() == null
            ? Optional.empty()
            : Optional.of(new SelectedVariation(
                e.getVariationId(), e.getVariationName(),
                Money.of(e.getVariationModifier(), Currency.getInstance(e.getCurrency()))));

        Optional<HalfAndHalf> halfAndHalf = e.getHalfLeftProductId() == null
            ? Optional.empty()
            : Optional.of(new HalfAndHalf(
                e.getHalfLeftProductId(), e.getHalfRightProductId(),
                e.getHalfDisplayName(),
                Money.of(e.getHalfBasePrice(), Currency.getInstance(e.getCurrency()))));

        List<SelectedAddOn> addOns = e.getAddOns().stream()
            .map(a -> new SelectedAddOn(
                a.getAddOnGroupId(), a.getAddOnItemId(), a.getName(),
                Money.of(a.getPrice(), Currency.getInstance(a.getCurrency())),
                a.getQuantity()))
            .toList();

        return new CartItem(
            CartItemId.of(e.getId()),
            e.getProductId(),
            variation,
            addOns,
            halfAndHalf,
            new Observation(e.getObservation()),
            e.getQuantity()
        );
    }
}
