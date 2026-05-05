package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.dto.AddOnGroupView;
import com.cardapio.catalog.application.dto.AddOnItemView;
import com.cardapio.catalog.application.dto.ProductDetailsView;
import com.cardapio.catalog.application.dto.VariationView;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetProductDetailsQuery {

    private final ProductRepository products;

    @Transactional(readOnly = true)
    public Result<ProductDetailsView> execute(ProductId id) {
        return Result.ofOptional(products.findById(id), ErrorCode.PRODUCT_NOT_FOUND)
            .map(p -> {
                var variations = p.variations().stream()
                    .map(v -> new VariationView(v.id(), v.name(), v.priceModifier()))
                    .toList();
                var groups = p.addOnGroups().stream()
                    .map(g -> new AddOnGroupView(g.id(), g.name(), g.minSelection(), g.maxSelection(),
                        g.items().stream().map(i -> new AddOnItemView(i.id(), i.name(), i.price())).toList()))
                    .toList();
                return new ProductDetailsView(
                    p.id(), p.categoryId(), p.name(), p.description(), p.basePrice(), p.imageUrl(),
                    p.isAvailable(), p.allowsHalfHalf(), p.stock().rawQuantity(), variations, groups);
            });
    }
}
