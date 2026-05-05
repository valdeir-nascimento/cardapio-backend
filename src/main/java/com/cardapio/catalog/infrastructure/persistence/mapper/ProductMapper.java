package com.cardapio.catalog.infrastructure.persistence.mapper;

import com.cardapio.catalog.domain.model.AddOnGroup;
import com.cardapio.catalog.domain.model.AddOnGroupId;
import com.cardapio.catalog.domain.model.AddOnItem;
import com.cardapio.catalog.domain.model.AddOnItemId;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.catalog.domain.model.Stock;
import com.cardapio.catalog.domain.model.Variation;
import com.cardapio.catalog.domain.model.VariationId;
import com.cardapio.catalog.infrastructure.persistence.jpa.AddOnGroupJpaEntity;
import com.cardapio.catalog.infrastructure.persistence.jpa.AddOnItemJpaEntity;
import com.cardapio.catalog.infrastructure.persistence.jpa.ProductJpaEntity;
import com.cardapio.catalog.infrastructure.persistence.jpa.VariationJpaEntity;
import com.cardapio.shared.domain.Money;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public final class ProductMapper {

    private ProductMapper() {}

    public static ProductJpaEntity toJpa(Product p, Instant now) {
        ProductJpaEntity entity = new ProductJpaEntity(
            p.id().value(), p.categoryId().value(), p.name(), p.description(),
            p.basePrice().amount(), p.basePrice().currency().getCurrencyCode(),
            p.imageUrl(), p.isAvailable(), p.allowsHalfHalf(),
            p.stock().rawQuantity(), now, now);
        populateVariations(entity, p);
        populateAddOnGroups(entity, p);
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
        populateVariations(entity, p);
        populateAddOnGroups(entity, p);
    }

    public static Product toDomain(ProductJpaEntity e) {
        Currency currency = Currency.getInstance(e.getCurrency());

        List<Variation> variations = e.getVariations().stream()
            .map(ve -> Variation.rehydrate(VariationId.of(ve.getId()), ve.getName(),
                Money.of(ve.getPriceModifier(), Currency.getInstance(ve.getCurrency()))))
            .toList();

        List<AddOnGroup> groups = e.getAddOnGroups().stream()
            .map(ge -> AddOnGroup.rehydrate(AddOnGroupId.of(ge.getId()), ge.getName(),
                ge.getMinSelection(), ge.getMaxSelection(),
                ge.getItems().stream()
                    .map(ie -> AddOnItem.rehydrate(AddOnItemId.of(ie.getId()), ie.getName(),
                        Money.of(ie.getPrice(), Currency.getInstance(ie.getCurrency()))))
                    .toList()))
            .toList();

        Stock stock = e.getStockQuantity() == null ? Stock.untracked() : Stock.of(e.getStockQuantity());

        return Product.rehydrate(
            ProductId.of(e.getId()), e.getName(), e.getDescription(),
            Money.of(e.getBasePrice(), currency),
            CategoryId.of(e.getCategoryId()), e.getImageUrl(),
            e.isAvailable(), e.isAllowsHalfHalf(), stock, variations, groups);
    }

    private static void populateVariations(ProductJpaEntity entity, Product p) {
        entity.getVariations().clear();
        UUID productId = p.id().value();
        int pos = 0;
        for (Variation v : p.variations()) {
            entity.getVariations().add(toVariationEntity(v, productId, pos++));
        }
    }

    private static VariationJpaEntity toVariationEntity(Variation v, UUID productId, int pos) {
        return new VariationJpaEntity(
            v.id().value(), productId, v.name(),
            v.priceModifier().amount(),
            v.priceModifier().currency().getCurrencyCode(), pos);
    }

    private static void populateAddOnGroups(ProductJpaEntity entity, Product p) {
        entity.getAddOnGroups().clear();
        UUID productId = p.id().value();
        int pos = 0;
        for (AddOnGroup g : p.addOnGroups()) {
            entity.getAddOnGroups().add(toAddOnGroupEntity(g, productId, pos++));
        }
    }

    private static AddOnGroupJpaEntity toAddOnGroupEntity(AddOnGroup g, UUID productId, int pos) {
        AddOnGroupJpaEntity ge = new AddOnGroupJpaEntity(
            g.id().value(), productId, g.name(), g.minSelection(), g.maxSelection(), pos);
        int ipos = 0;
        for (AddOnItem item : g.items()) {
            ge.getItems().add(toAddOnItemEntity(item, g.id().value(), ipos++));
        }
        return ge;
    }

    private static AddOnItemJpaEntity toAddOnItemEntity(AddOnItem item, UUID groupId, int pos) {
        return new AddOnItemJpaEntity(
            item.id().value(), groupId, item.name(),
            item.price().amount(),
            item.price().currency().getCurrencyCode(), pos);
    }
}
