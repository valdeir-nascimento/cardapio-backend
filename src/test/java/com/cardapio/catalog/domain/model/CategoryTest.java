package com.cardapio.catalog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Test
    void createsActiveCategory() {
        Category category = Category.create("Pizzas", 1);
        assertThat(category.id()).isNotNull();
        assertThat(category.name()).isEqualTo("Pizzas");
        assertThat(category.displayOrder()).isEqualTo(1);
        assertThat(category.isActive()).isTrue();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Category.create("  ", 1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeDisplayOrder() {
        assertThatThrownBy(() -> Category.create("X", -1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canBeRenamedAndReordered() {
        Category category = Category.create("Old", 1);
        category.rename("New");
        category.reorder(5);
        assertThat(category.name()).isEqualTo("New");
        assertThat(category.displayOrder()).isEqualTo(5);
    }

    @Test
    void deactivateAndReactivate() {
        Category category = Category.create("X", 1);
        category.deactivate();
        assertThat(category.isActive()).isFalse();
        category.activate();
        assertThat(category.isActive()).isTrue();
    }
}
