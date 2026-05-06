package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.dto.CartItemView;
import com.cardapio.ordering.application.dto.CartView;
import com.cardapio.ordering.domain.model.Cart;
import com.cardapio.ordering.domain.model.CartItem;
import com.cardapio.ordering.domain.model.HalfAndHalf;
import com.cardapio.ordering.domain.model.SelectedAddOn;
import com.cardapio.ordering.domain.model.SelectedVariation;
import com.cardapio.ordering.domain.port.CartRepository;
import com.cardapio.ordering.domain.port.CatalogQueryPort;
import com.cardapio.ordering.domain.port.CatalogQueryPort.ProductSnapshot;
import com.cardapio.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetCartQuery {

    private final CartRepository cartRepo;
    private final CatalogQueryPort catalog;
    private final java.time.Clock clock;

    @Transactional(readOnly = true)
    public CartView getOrEmpty(UUID customerId) {
        Cart cart = cartRepo.findByCustomerId(customerId).orElseGet(() -> Cart.createEmpty(customerId, clock));

        List<CartItemView> views = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean hasUnavailable = false;
        Currency currency = Currency.getInstance("BRL");

        for (CartItem ci : cart.items()) {
            ProductSnapshot snap = catalog.loadProduct(ci.productId()).orElse(null);
            boolean available = snap != null && snap.available()
                && (!snap.stockTracked() || snap.stockQuantity() >= ci.quantity());
            if (!available) hasUnavailable = true;

            String productName = snap != null ? snap.name() : "(produto indisponível)";
            BigDecimal lineTotal = BigDecimal.ZERO;
            if (snap != null) {
                Money base = ci.halfAndHalf()
                    .map(h -> h.basePrice().amount().compareTo(snap.basePrice().amount()) >= 0
                        ? h.basePrice() : snap.basePrice())
                    .orElse(snap.basePrice());
                Money unit = base;
                if (ci.variation().isPresent()) {
                    unit = unit.add(ci.variation().get().priceModifier());
                }
                for (SelectedAddOn ao : ci.addOns()) {
                    unit = unit.add(ao.price().multiply(ao.quantity()));
                }
                lineTotal = unit.multiply(ci.quantity()).amount();
                subtotal = subtotal.add(lineTotal);
                currency = snap.basePrice().currency();
            }

            String variationName = ci.variation().map(SelectedVariation::name).orElse(null);
            List<String> addOnNames = ci.addOns().stream().map(SelectedAddOn::name).toList();
            String halfDescription = ci.halfAndHalf().map(HalfAndHalf::displayName).orElse(null);

            views.add(new CartItemView(
                ci.id().value(), ci.productId(), productName,
                variationName, addOnNames, halfDescription,
                ci.observation().value(), ci.quantity(), lineTotal, available
            ));
        }

        return new CartView(cart.id().value(), customerId, views, subtotal,
            currency.getCurrencyCode(), hasUnavailable);
    }
}
