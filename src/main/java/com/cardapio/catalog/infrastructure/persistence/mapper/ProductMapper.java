package com.cardapio.catalog.infrastructure.persistence.mapper;

import com.cardapio.catalog.domain.model.*;
import com.cardapio.catalog.infrastructure.persistence.jpa.*;
import com.cardapio.shared.domain.Money;

import java.time.Instant;
import java.util.Currency;
import java.util.List;

public final class ProductMapper {
    private ProductMapper() {}

    public static ProductJpaEntity toJpa(Product p, Instant now) {
        ProductJpaEntity entity = new ProductJpaEntity(
            p.id().value(), p.categoryId().value(), p.name(), p.description(),
            p.basePrice().amount(), p.basePrice().currency().getCurrencyCode(),
            p.imageUrl(), p.isAvailable(), p.allowsHalfHalf(),
            p.stock().rawQuantity(), now, now);

        int pos = 0;
        for (Variation v : p.variations()) {
            entity.getVariations().add(new VariationJpaEntity(
                v.id().value(), p.id().value(), v.name(),
                v.priceModifier().amount(), v.priceModifier().currency().getCurrencyCode(), pos++));
        }
        pos = 0;
        for (AddOnGroup g : p.addOnGroups()) {
            AddOnGroupJpaEntity ge = new AddOnGroupJpaEntity(
                g.id().value(), p.id().value(), g.name(), g.minSelection(), g.maxSelection(), pos++);
            int ipos = 0;
            for (AddOnItem item : g.items()) {
                ge.getItems().add(new AddOnItemJpaEntity(
                    item.id().value(), g.id().value(), item.name(),
                    item.price().amount(), item.price().currency().getCurrencyCode(), ipos++));
            }
            entity.getAddOnGroups().add(ge);
        }
        return entity;
    }

    public static void updateJpa(ProductJpaEntity entity, Product p, Instant now) {
        entity.setCategoryId(p.categoryId().value());
        entity.setName(p.name());
        entity.setDescription(p.description());
        entity.setBasePrice(p.basePrice().amount());
        entity.setImageUrl(p.imageUrl());
        entity.setAvailable(p.isAvailable());
        entity.setAllowsHalfHalf(p.allowsHalfHalf());
        entity.setStockQuantity(p.stock().rawQuantity());
        entity.setUpdatedAt(now);

        // simple replacement strategy (orphanRemoval handles deletes)
        entity.getVariations().clear();
        int pos = 0;
        for (Variation v : p.variations()) {
            entity.getVariations().add(new VariationJpaEntity(
                v.id().value(), p.id().value(), v.name(),
                v.priceModifier().amount(), v.priceModifier().currency().getCurrencyCode(), pos++));
        }

        entity.getAddOnGroups().clear();
        pos = 0;
        for (AddOnGroup g : p.addOnGroups()) {
            AddOnGroupJpaEntity ge = new AddOnGroupJpaEntity(
                g.id().value(), p.id().value(), g.name(), g.minSelection(), g.maxSelection(), pos++);
            int ipos = 0;
            for (AddOnItem item : g.items()) {
                ge.getItems().add(new AddOnItemJpaEntity(
                    item.id().value(), g.id().value(), item.name(),
                    item.price().amount(), item.price().currency().getCurrencyCode(), ipos++));
            }
            entity.getAddOnGroups().add(ge);
        }
    }

    public static Product toDomain(ProductJpaEntity e) {
        Currency currency = Currency.getInstance(e.getCurrency());

        List<Variation> variations = e.getVariations().stream()
            .map(ve -> Variation.rehydrate(VariationId.of(ve.getId()), ve.getName(),
                Money.of(ve.getPriceModifier(), Currency.getInstance(ve.getCurrency()))))
            .toList();

        List<AddOnGroup> groups = e.getAddOnGroups().stream()
            .map(ge -> {
                List<AddOnItem> items = ge.getItems().stream()
                    .map(ie -> AddOnItem.rehydrate(AddOnItemId.of(ie.getId()), ie.getName(),
                        Money.of(ie.getPrice(), Currency.getInstance(ie.getCurrency()))))
                    .toList();
                return AddOnGroup.rehydrate(AddOnGroupId.of(ge.getId()), ge.getName(),
                    ge.getMinSelection(), ge.getMaxSelection(), items);
            })
            .toList();

        Stock stock = e.getStockQuantity() == null ? Stock.untracked() : Stock.of(e.getStockQuantity());

        return Product.rehydrate(
            ProductId.of(e.getId()), e.getName(), e.getDescription(),
            Money.of(e.getBasePrice(), currency),
            CategoryId.of(e.getCategoryId()), e.getImageUrl(),
            e.isAvailable(), e.isAllowsHalfHalf(), stock, variations, groups);
    }
}
