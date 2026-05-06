package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.domain.model.Cart;
import com.cardapio.ordering.domain.model.CartItem;
import com.cardapio.ordering.domain.model.OrderItem;
import com.cardapio.ordering.domain.port.CatalogQueryPort;
import com.cardapio.ordering.domain.port.CatalogQueryPort.ProductSnapshot;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Re-prices a Cart against the current catalog and produces frozen OrderItems.
 * Returns Result.Failure with a Notification listing all unavailable / out-of-stock items
 * if any item fails validation.
 */
@Service
@RequiredArgsConstructor
public class CartPricingService {

    private final CatalogQueryPort catalog;

    public Result<List<OrderItem>> price(Cart cart) {
        Notification n = Notification.empty();
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem ci : cart.items()) {
            ProductSnapshot snap = catalog.loadProduct(ci.productId()).orElse(null);
            if (snap == null) {
                n.addError("cart.item:" + ci.id().value(), ErrorCode.PRODUCT_NOT_FOUND);
                continue;
            }
            if (!snap.available()) {
                n.addError("cart.item:" + ci.id().value(), ErrorCode.PRODUCT_UNAVAILABLE,
                    "Produto indisponível: " + snap.name());
                continue;
            }
            if (snap.stockTracked() && snap.stockQuantity() < ci.quantity()) {
                n.addError("cart.item:" + ci.id().value(), ErrorCode.PRODUCT_OUT_OF_STOCK,
                    "Estoque insuficiente para " + snap.name());
                continue;
            }

            orderItems.add(OrderItem.create(
                snap.productId(),
                snap.name(),
                snap.basePrice(),
                ci.variation(),
                ci.addOns(),
                ci.halfAndHalf(),
                ci.observation(),
                ci.quantity()
            ));
        }

        if (n.hasErrors()) return Result.failure(n);
        return Result.success(List.copyOf(orderItems));
    }
}
