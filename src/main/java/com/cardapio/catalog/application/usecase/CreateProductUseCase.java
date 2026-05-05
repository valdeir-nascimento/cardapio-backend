package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.CreateProductCommand;
import com.cardapio.catalog.domain.model.*;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProductUseCase {
    private final ProductRepository products;
    private final CategoryRepository categories;

    public CreateProductUseCase(ProductRepository products, CategoryRepository categories) {
        this.products = products; this.categories = categories;
    }

    @Transactional
    public Result<ProductId> execute(CreateProductCommand cmd) {
        Notification n = Notification.empty();
        if (cmd.name() == null || cmd.name().isBlank()) n.addError("name", "BLANK_NAME", "nome obrigatório");
        if (cmd.basePrice() == null) n.addError("basePrice", "INVALID_PRICE", "preço inválido");
        if (cmd.categoryId() == null || !categories.existsById(cmd.categoryId())) {
            n.addError("categoryId", "CATEGORY_NOT_FOUND", "categoria não existe");
        }
        if (n.hasErrors()) return Result.failure(n);

        Product p = Product.create(cmd.name(), cmd.description(), cmd.basePrice(),
            cmd.categoryId(), cmd.imageUrl(), cmd.allowsHalfHalf());

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
