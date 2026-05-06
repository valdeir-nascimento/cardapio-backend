package com.cardapio.ordering.domain.port;

import com.cardapio.shared.domain.Money;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogQueryPort {

    Optional<ProductSnapshot> loadProduct(UUID productId);

    record ProductSnapshot(
        UUID productId,
        String name,
        Money basePrice,
        boolean available,
        boolean stockTracked,
        int stockQuantity,
        boolean allowsHalfHalf,
        List<VariationSnapshot> variations,
        List<AddOnGroupSnapshot> addOnGroups
    ) {
        public Optional<VariationSnapshot> findVariation(UUID id) {
            return variations.stream().filter(v -> v.id().equals(id)).findFirst();
        }

        public Optional<AddOnGroupSnapshot> findGroup(UUID id) {
            return addOnGroups.stream().filter(g -> g.id().equals(id)).findFirst();
        }
    }

    record VariationSnapshot(UUID id, String name, Money priceModifier) {}

    record AddOnGroupSnapshot(UUID id, String name, int minSelection, int maxSelection,
                              List<AddOnItemSnapshot> items) {
        public Optional<AddOnItemSnapshot> findItem(UUID itemId) {
            return items.stream().filter(i -> i.id().equals(itemId)).findFirst();
        }
    }

    record AddOnItemSnapshot(UUID id, String name, Money price) {}
}
