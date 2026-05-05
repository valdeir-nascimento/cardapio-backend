package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddOnGroupTest {

    @Test
    void createsEmptyGroup() {
        AddOnGroup g = AddOnGroup.create("Adicionais", 0, 5);
        assertThat(g.items()).isEmpty();
        assertThat(g.minSelection()).isEqualTo(0);
        assertThat(g.maxSelection()).isEqualTo(5);
    }

    @Test
    void addsAndRemovesItems() {
        AddOnGroup g = AddOnGroup.create("Adicionais", 0, 5);
        AddOnItem bacon = AddOnItem.create("Bacon", Money.brl("3.00"));
        AddOnItem cheese = AddOnItem.create("Queijo extra", Money.brl("2.50"));
        g.addItem(bacon);
        g.addItem(cheese);
        assertThat(g.items()).hasSize(2);

        g.removeItem(bacon.id());
        assertThat(g.items()).hasSize(1);
        assertThat(g.items().get(0).name()).isEqualTo("Queijo extra");
    }

    @Test
    void itemsListIsImmutableFromOutside() {
        AddOnGroup g = AddOnGroup.create("X", 0, 1);
        var items = g.items();
        assertThatThrownBy(() -> items.add(AddOnItem.create("Y", Money.brl("1.00"))))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsMaxLessThanMin() {
        assertThatThrownBy(() -> AddOnGroup.create("X", 3, 1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
