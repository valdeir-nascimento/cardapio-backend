package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.CreateProductCommand;
import com.cardapio.catalog.application.command.UpdateProductCommand;
import com.cardapio.catalog.domain.model.*;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UpdateProductUseCase {
    private final ProductRepository products;
    private final CategoryRepository categories;

    public UpdateProductUseCase(ProductRepository products, CategoryRepository categories) {
        this.products = products; this.categories = categories;
    }

    @Transactional
    public Result<ProductId> execute(UpdateProductCommand cmd) {
        Notification n = Notification.empty();
        Optional<Product> maybe = products.findById(cmd.id());
        if (maybe.isEmpty()) { n.addError("PRODUCT_NOT_FOUND", "produto não encontrado"); return Result.failure(n); }
        if (cmd.name() == null || cmd.name().isBlank()) n.addError("name", "BLANK_NAME", "nome obrigatório");
        if (cmd.basePrice() == null) n.addError("basePrice", "INVALID_PRICE", "preço inválido");
        if (cmd.categoryId() == null || !categories.existsById(cmd.categoryId())) {
            n.addError("categoryId", "CATEGORY_NOT_FOUND", "categoria não existe");
        }
        if (n.hasErrors()) return Result.failure(n);

        Product p = maybe.get();
        p.rename(cmd.name());
        p.changeDescription(cmd.description());
        p.repriceBase(cmd.basePrice());
        p.moveToCategory(cmd.categoryId());
        p.changeImage(cmd.imageUrl());
        if (cmd.allowsHalfHalf()) p.allowHalfHalf(); else p.disallowHalfHalf();

        // wholesale replacement of variations + addons
        for (Variation v : p.variations().stream().toList()) p.removeVariation(v.id());
        for (AddOnGroup g : p.addOnGroups().stream().toList()) p.removeAddOnGroup(g.id());

        if (cmd.variations() != null) {
            for (CreateProductCommand.VariationDraft v : cmd.variations()) {
                p.addVariation(Variation.create(v.name(), v.priceModifier()));
            }
        }
        if (cmd.addOnGroups() != null) {
            for (CreateProductCommand.AddOnGroupDraft g : cmd.addOnGroups()) {
                AddOnGroup group = AddOnGroup.create(g.name(), g.minSelection(), g.maxSelection());
                if (g.items() != null) {
                    for (CreateProductCommand.AddOnItemDraft item : g.items()) {
                        group.addItem(AddOnItem.create(item.name(), item.price()));
                    }
                }
                p.addAddOnGroup(group);
            }
        }
        products.save(p);
        return Result.success(p.id());
    }
}
