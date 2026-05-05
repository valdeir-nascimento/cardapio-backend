package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.CreateCategoryCommand;
import com.cardapio.catalog.application.command.UpdateCategoryCommand;
import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoryUseCasesTest {

    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);

    @Test
    void createValid() {
        Result<CategoryId> r = new CreateCategoryUseCase(categories).execute(new CreateCategoryCommand("Pizzas", 1));
        assertThat(r.isSuccess()).isTrue();
        verify(categories).save(any());
    }

    @Test
    void createRejectsBlankName() {
        Result<CategoryId> r = new CreateCategoryUseCase(categories).execute(new CreateCategoryCommand("  ", 1));
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    void updateExistingCategory() {
        CategoryId id = CategoryId.newId();
        Category existing = Category.rehydrate(id, "Old", 1, true);
        when(categories.findById(id)).thenReturn(Optional.of(existing));

        Result<CategoryId> r = new UpdateCategoryUseCase(categories).execute(
            new UpdateCategoryCommand(id, "New", 2, false));

        assertThat(r.isSuccess()).isTrue();
        verify(categories).save(existing);
        assertThat(existing.name()).isEqualTo("New");
        assertThat(existing.isActive()).isFalse();
    }

    @Test
    void updateMissingCategoryFails() {
        when(categories.findById(any())).thenReturn(Optional.empty());
        Result<CategoryId> r = new UpdateCategoryUseCase(categories).execute(
            new UpdateCategoryCommand(CategoryId.newId(), "X", 1, true));
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    void deleteEmptyCategory() {
        CategoryId id = CategoryId.newId();
        when(categories.existsById(id)).thenReturn(true);
        when(products.countByCategory(id)).thenReturn(0L);

        Result<Void> r = new DeleteCategoryUseCase(categories, products).execute(id);
        assertThat(r.isSuccess()).isTrue();
        verify(categories).deleteById(id);
    }

    @Test
    void deleteRejectsCategoryWithProducts() {
        CategoryId id = CategoryId.newId();
        when(categories.existsById(id)).thenReturn(true);
        when(products.countByCategory(id)).thenReturn(3L);

        Result<Void> r = new DeleteCategoryUseCase(categories, products).execute(id);
        assertThat(r.isSuccess()).isFalse();
        verify(categories, never()).deleteById(any());
    }
}
