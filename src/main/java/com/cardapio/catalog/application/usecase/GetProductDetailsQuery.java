// GetProductDetailsQuery.java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.dto.*;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GetProductDetailsQuery {
    private final ProductRepository products;
    public GetProductDetailsQuery(ProductRepository products) { this.products = products; }

    @Transactional(readOnly = true)
    public Result<ProductDetailsView> execute(ProductId id) {
        Optional<Product> maybe = products.findById(id);
        if (maybe.isEmpty()) {
            Notification n = Notification.empty();
            n.addError("PRODUCT_NOT_FOUND", "produto não encontrado");
            return Result.failure(n);
        }
        Product p = maybe.get();
        var variations = p.variations().stream()
            .map(v -> new VariationView(v.id(), v.name(), v.priceModifier()))
            .toList();
        var groups = p.addOnGroups().stream()
            .map(g -> new AddOnGroupView(g.id(), g.name(), g.minSelection(), g.maxSelection(),
                g.items().stream().map(i -> new AddOnItemView(i.id(), i.name(), i.price())).toList()))
            .toList();
        return Result.success(new ProductDetailsView(
            p.id(), p.categoryId(), p.name(), p.description(), p.basePrice(), p.imageUrl(),
            p.isAvailable(), p.allowsHalfHalf(), p.stock().rawQuantity(), variations, groups));
    }
}
