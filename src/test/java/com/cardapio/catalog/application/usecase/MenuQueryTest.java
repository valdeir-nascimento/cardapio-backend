package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.dto.MenuView;
import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MenuQueryTest {

    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);

    @Test
    void returnsActiveCategoriesWithAvailableProducts() {
        CategoryId catId = CategoryId.newId();
        Category cat = Category.rehydrate(catId, "Pizzas", 1, true);
        Product pizza = Product.create("Margherita", "desc", Money.brl("39.90"), catId, null, false);

        when(categories.findAllActive()).thenReturn(List.of(cat));
        when(products.findAvailableGroupedByCategories(List.of(catId))).thenReturn(Map.of(catId, List.of(pizza)));

        MenuView menu = new GetMenuQuery(categories, products).execute();
        assertThat(menu.categories()).hasSize(1);
        assertThat(menu.categories().get(0).products()).hasSize(1);
        assertThat(menu.categories().get(0).products().get(0).name()).isEqualTo("Margherita");
    }
}
