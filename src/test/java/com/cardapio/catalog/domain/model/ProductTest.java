package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void createsAvailableProductWithBasePrice() {
        CategoryId category = CategoryId.newId();
        Product p = Product.create(
            "Pizza Margherita", "Molho, mussarela, manjericão",
            Money.brl("39.90"), category, null, false);

        assertThat(p.id()).isNotNull();
        assertThat(p.name()).isEqualTo("Pizza Margherita");
        assertThat(p.basePrice()).isEqualTo(Money.brl("39.90"));
        assertThat(p.categoryId()).isEqualTo(category);
        assertThat(p.isAvailable()).isTrue();
        assertThat(p.allowsHalfHalf()).isFalse();
        assertThat(p.stock().isTracked()).isFalse();
        assertThat(p.variations()).isEmpty();
        assertThat(p.addOnGroups()).isEmpty();
    }

    @Test
    void allowsHalfHalfWhenFlagged() {
        Product p = Product.create("Pizza", "desc", Money.brl("39.90"),
            CategoryId.newId(), null, true);
        assertThat(p.allowsHalfHalf()).isTrue();
    }

    @Test
    void supportsVariationsAndAddOns() {
        Product p = Product.create("Pizza", "desc", Money.brl("39.90"),
            CategoryId.newId(), null, true);

        Variation small = Variation.create("Pequena", Money.brl("0.00"));
        Variation large = Variation.create("Grande", Money.brl("10.00"));
        p.addVariation(small);
        p.addVariation(large);

        AddOnGroup extras = AddOnGroup.create("Adicionais", 0, 3);
        extras.addItem(AddOnItem.create("Bacon", Money.brl("3.00")));
        p.addAddOnGroup(extras);

        assertThat(p.variations()).hasSize(2);
        assertThat(p.addOnGroups()).hasSize(1);
    }

    @Test
    void availabilityToggle() {
        Product p = Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false);
        p.markUnavailable();
        assertThat(p.isAvailable()).isFalse();
        p.markAvailable();
        assertThat(p.isAvailable()).isTrue();
    }

    @Test
    void setStockReplacesValue() {
        Product p = Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false);
        p.changeStock(Stock.of(50));
        assertThat(p.stock().quantity()).isEqualTo(50);
        p.changeStock(Stock.untracked());
        assertThat(p.stock().isTracked()).isFalse();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Product.create("  ", "desc", Money.brl("1.00"), CategoryId.newId(), null, false))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeVariation() {
        Product p = Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false);
        Variation v = Variation.create("M", Money.brl("0.00"));
        p.addVariation(v);
        p.removeVariation(v.id());
        assertThat(p.variations()).isEmpty();
    }
}
