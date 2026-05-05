package com.cardapio.catalog.api.assembler;

import com.cardapio.catalog.api.dto.AddOnGroupRequest;
import com.cardapio.catalog.api.dto.ProductRequest;
import com.cardapio.catalog.api.dto.VariationRequest;
import com.cardapio.catalog.application.command.CreateProductCommand;
import com.cardapio.catalog.application.command.ProductDrafts;
import com.cardapio.catalog.application.command.UpdateProductCommand;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.shared.domain.Money;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class ProductCommandAssembler {

    public CreateProductCommand toCreateCommand(ProductRequest req) {
        return new CreateProductCommand(
            req.name(),
            Objects.requireNonNullElse(req.description(), ""),
            Money.brl(req.basePrice()),
            CategoryId.of(req.categoryId()),
            req.imageUrl(),
            req.allowsHalfHalf(),
            toVariationDrafts(req.variations()),
            toAddOnDrafts(req.addOnGroups()));
    }

    public UpdateProductCommand toUpdateCommand(UUID id, ProductRequest req) {
        return new UpdateProductCommand(
            ProductId.of(id),
            req.name(),
            Objects.requireNonNullElse(req.description(), ""),
            Money.brl(req.basePrice()),
            CategoryId.of(req.categoryId()),
            req.imageUrl(),
            req.allowsHalfHalf(),
            toVariationDrafts(req.variations()),
            toAddOnDrafts(req.addOnGroups()));
    }

    private List<ProductDrafts.VariationDraft> toVariationDrafts(List<VariationRequest> vs) {
        if (vs == null) return List.of();
        return vs.stream()
            .map(v -> new ProductDrafts.VariationDraft(v.name(), Money.brl(v.priceModifier())))
            .toList();
    }

    private List<ProductDrafts.AddOnGroupDraft> toAddOnDrafts(List<AddOnGroupRequest> gs) {
        if (gs == null) return List.of();
        return gs.stream()
            .map(g -> new ProductDrafts.AddOnGroupDraft(
                g.name(), g.minSelection(), g.maxSelection(),
                g.items() == null ? List.of() : g.items().stream()
                    .map(i -> new ProductDrafts.AddOnItemDraft(i.name(), Money.brl(i.price())))
                    .toList()))
            .toList();
    }
}
