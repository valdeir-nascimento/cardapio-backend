package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.CreateProductCommand;
import com.cardapio.catalog.application.command.ProductDrafts;
import com.cardapio.catalog.domain.model.AddOnGroup;
import com.cardapio.catalog.domain.model.AddOnItem;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.catalog.domain.model.Variation;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateProductUseCase {

    private final ProductRepository products;
    private final ProductCommandValidator validator;

    @Transactional
    public Result<ProductId> execute(CreateProductCommand cmd) {
        Notification n = Notification.empty();
        validator.validate(cmd.name(), cmd.basePrice(), cmd.categoryId(), n);
        if (n.hasErrors()) return Result.failure(n);

        Product p = Product.create(cmd.name(), cmd.description(), cmd.basePrice(),
            cmd.categoryId(), cmd.imageUrl(), cmd.allowsHalfHalf());

        for (ProductDrafts.VariationDraft v : cmd.variations()) {
            p.addVariation(Variation.create(v.name(), v.priceModifier()));
        }
        for (ProductDrafts.AddOnGroupDraft g : cmd.addOnGroups()) {
            AddOnGroup group = AddOnGroup.create(g.name(), g.minSelection(), g.maxSelection());
            for (ProductDrafts.AddOnItemDraft item : g.items()) {
                group.addItem(AddOnItem.create(item.name(), item.price()));
            }
            p.addAddOnGroup(group);
        }

        products.save(p);
        return Result.success(p.id());
    }
}
