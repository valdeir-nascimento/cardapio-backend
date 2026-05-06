package com.cardapio.ordering.infrastructure.catalog;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.catalog.application.CatalogFacade;
import com.cardapio.catalog.application.dto.AddOnGroupView;
import com.cardapio.catalog.application.dto.AddOnItemView;
import com.cardapio.catalog.application.dto.ProductDetailsView;
import com.cardapio.catalog.application.dto.VariationView;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.ordering.domain.port.CatalogQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CatalogQueryAdapter implements CatalogQueryPort {

    private final CatalogFacade catalog;

    @Override
    public Optional<ProductSnapshot> loadProduct(UUID productId) {
        try {
            ProductDetailsView view = catalog.getProductDetails(ProductId.of(productId));
            return Optional.of(toSnapshot(view));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    private ProductSnapshot toSnapshot(ProductDetailsView v) {
        boolean tracked = v.stockQuantity() != null;
        int qty = tracked ? v.stockQuantity() : 0;
        return new ProductSnapshot(
            v.id().value(),
            v.name(),
            v.basePrice(),
            v.available(),
            tracked,
            qty,
            v.allowsHalfHalf(),
            v.variations().stream().map(this::toVariation).toList(),
            v.addOnGroups().stream().map(this::toGroup).toList()
        );
    }

    private VariationSnapshot toVariation(VariationView v) {
        return new VariationSnapshot(v.id().value(), v.name(), v.priceModifier());
    }

    private AddOnGroupSnapshot toGroup(AddOnGroupView g) {
        return new AddOnGroupSnapshot(
            g.id().value(), g.name(), g.minSelection(), g.maxSelection(),
            g.items().stream().map(this::toItem).toList()
        );
    }

    private AddOnItemSnapshot toItem(AddOnItemView i) {
        return new AddOnItemSnapshot(i.id().value(), i.name(), i.price());
    }
}
