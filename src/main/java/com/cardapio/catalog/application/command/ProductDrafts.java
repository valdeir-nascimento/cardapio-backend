package com.cardapio.catalog.application.command;

import com.cardapio.shared.domain.Money;

import java.util.List;

public final class ProductDrafts {

    private ProductDrafts() {}

    public record VariationDraft(String name, Money priceModifier) {}

    public record AddOnGroupDraft(
        String name,
        int minSelection,
        int maxSelection,
        List<AddOnItemDraft> items
    ) {
        public AddOnGroupDraft {
            items = items != null ? List.copyOf(items) : List.of();
        }
    }

    public record AddOnItemDraft(String name, Money price) {}
}
