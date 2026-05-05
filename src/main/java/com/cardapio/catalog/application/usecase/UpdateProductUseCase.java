package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.ProductDrafts;
import com.cardapio.catalog.application.command.UpdateProductCommand;
import com.cardapio.catalog.domain.model.AddOnGroup;
import com.cardapio.catalog.domain.model.AddOnItem;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.catalog.domain.model.Variation;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProductUseCase extends WriteProductUseCase {

    private final ProductCommandValidator validator;

    public UpdateProductUseCase(ProductRepository products, ProductCommandValidator validator) {
        super(products);
        this.validator = validator;
    }

    @Transactional
    public Result<ProductId> execute(UpdateProductCommand cmd) {
        return loadProduct(cmd.id()).flatMap(p -> {
            Notification n = Notification.empty();
            validator.validate(cmd.name(), cmd.basePrice(), cmd.categoryId(), n);
            if (n.hasErrors()) return Result.failure(n);

            p.rename(cmd.name());
            p.changeDescription(cmd.description());
            p.repriceBase(cmd.basePrice());
            p.moveToCategory(cmd.categoryId());
            p.changeImage(cmd.imageUrl());
            if (cmd.allowsHalfHalf()) p.allowHalfHalf(); else p.disallowHalfHalf();

            for (Variation v : p.variations().stream().toList()) p.removeVariation(v.id());
            for (AddOnGroup g : p.addOnGroups().stream().toList()) p.removeAddOnGroup(g.id());

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
        });
    }
}
