package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.command.AddCartItemCommand;
import com.cardapio.ordering.domain.model.HalfAndHalf;
import com.cardapio.ordering.domain.model.Observation;
import com.cardapio.ordering.domain.model.SelectedAddOn;
import com.cardapio.ordering.domain.model.SelectedVariation;
import com.cardapio.ordering.domain.port.CatalogQueryPort;
import com.cardapio.ordering.domain.port.CatalogQueryPort.AddOnGroupSnapshot;
import com.cardapio.ordering.domain.port.CatalogQueryPort.AddOnItemSnapshot;
import com.cardapio.ordering.domain.port.CatalogQueryPort.ProductSnapshot;
import com.cardapio.ordering.domain.port.CatalogQueryPort.VariationSnapshot;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Validates an AddCartItemCommand against a fresh catalog ProductSnapshot and
 * builds the snapshot value-objects (variation/addons/half) that the cart will store.
 */
public final class CartItemFactory {

    public record Built(
        java.util.UUID productId,
        Optional<SelectedVariation> variation,
        List<SelectedAddOn> addOns,
        Optional<HalfAndHalf> halfAndHalf,
        Observation observation,
        int quantity
    ) {}

    private CartItemFactory() {}

    public static com.cardapio.shared.domain.Result<Built> build(
            AddCartItemCommand cmd,
            CatalogQueryPort catalog
    ) {
        Notification n = Notification.empty();

        if (cmd.quantity() < 1) {
            n.addError("quantity", ErrorCode.INVALID_QUANTITY);
            return com.cardapio.shared.domain.Result.failure(n);
        }

        ProductSnapshot product = catalog.loadProduct(cmd.productId()).orElse(null);
        if (product == null) {
            return com.cardapio.shared.domain.Result.failWith(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!product.available()) {
            return com.cardapio.shared.domain.Result.failWith(ErrorCode.PRODUCT_UNAVAILABLE);
        }
        if (product.stockTracked() && product.stockQuantity() < cmd.quantity()) {
            return com.cardapio.shared.domain.Result.failWith(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }

        boolean hasHalf = cmd.halfLeftProductId() != null || cmd.halfRightProductId() != null;
        if (hasHalf && cmd.variationId() != null) {
            n.addError("halfAndHalf", ErrorCode.HALF_AND_HALF_INCOMPATIBLE_WITH_VARIATION);
            return com.cardapio.shared.domain.Result.failure(n);
        }

        Optional<SelectedVariation> variation = Optional.empty();
        if (cmd.variationId() != null) {
            VariationSnapshot vs = product.findVariation(cmd.variationId()).orElse(null);
            if (vs == null) {
                n.addError("variationId", ErrorCode.VARIATION_NOT_FOUND);
                return com.cardapio.shared.domain.Result.failure(n);
            }
            variation = Optional.of(new SelectedVariation(vs.id(), vs.name(), vs.priceModifier()));
        }

        Optional<HalfAndHalf> halfAndHalf = Optional.empty();
        if (hasHalf) {
            if (!product.allowsHalfHalf()) {
                n.addError("halfAndHalf", ErrorCode.HALF_AND_HALF_NOT_ALLOWED);
                return com.cardapio.shared.domain.Result.failure(n);
            }
            if (cmd.halfLeftProductId() == null || cmd.halfRightProductId() == null
                || cmd.halfLeftProductId().equals(cmd.halfRightProductId())) {
                n.addError("halfAndHalf", ErrorCode.HALF_AND_HALF_REQUIRES_TWO_PRODUCTS);
                return com.cardapio.shared.domain.Result.failure(n);
            }
            ProductSnapshot left = catalog.loadProduct(cmd.halfLeftProductId()).orElse(null);
            ProductSnapshot right = catalog.loadProduct(cmd.halfRightProductId()).orElse(null);
            if (left == null || right == null) {
                n.addError("halfAndHalf", ErrorCode.PRODUCT_NOT_FOUND);
                return com.cardapio.shared.domain.Result.failure(n);
            }
            // higher base price wins
            var basePrice = left.basePrice().amount().compareTo(right.basePrice().amount()) >= 0
                ? left.basePrice() : right.basePrice();
            halfAndHalf = Optional.of(new HalfAndHalf(
                left.productId(), right.productId(),
                left.name() + " / " + right.name(),
                basePrice
            ));
        }

        // Validate add-on selections
        List<SelectedAddOn> addOns = new java.util.ArrayList<>();
        Map<java.util.UUID, Integer> qtyPerGroup = new HashMap<>();
        if (cmd.addOns() != null) {
            for (var sel : cmd.addOns()) {
                AddOnGroupSnapshot group = product.findGroup(sel.groupId()).orElse(null);
                if (group == null) {
                    n.addError("addOns.groupId", ErrorCode.ADDON_GROUP_NOT_FOUND);
                    continue;
                }
                AddOnItemSnapshot item = group.findItem(sel.itemId()).orElse(null);
                if (item == null) {
                    n.addError("addOns.itemId", ErrorCode.ADDON_ITEM_NOT_FOUND);
                    continue;
                }
                if (sel.quantity() < 1) {
                    n.addError("addOns.quantity", ErrorCode.INVALID_QUANTITY);
                    continue;
                }
                qtyPerGroup.merge(group.id(), sel.quantity(), Integer::sum);
                addOns.add(new SelectedAddOn(group.id(), item.id(), item.name(), item.price(), sel.quantity()));
            }
        }
        // verify min/max per group
        for (AddOnGroupSnapshot g : product.addOnGroups()) {
            int total = qtyPerGroup.getOrDefault(g.id(), 0);
            if (total < g.minSelection() || total > g.maxSelection()) {
                if (g.minSelection() == 0 && total == 0) continue;
                n.addError("addOns.group:" + g.id(), ErrorCode.ADDON_GROUP_SELECTION_OUT_OF_RANGE);
            }
        }

        if (n.hasErrors()) return com.cardapio.shared.domain.Result.failure(n);

        return com.cardapio.shared.domain.Result.success(new Built(
            product.productId(), variation, addOns, halfAndHalf,
            Observation.of(cmd.observation()), cmd.quantity()
        ));
    }
}
