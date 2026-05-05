package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.*;
import com.cardapio.catalog.domain.model.*;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Money;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductUseCasesTest {

    private final ProductRepository products = mock(ProductRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);

    @Test
    void createsProductWithVariationsAndAddOns() {
        when(categories.existsById(any())).thenReturn(true);

        var cmd = new CreateProductCommand(
            "Pizza", "Mussarela", Money.brl("39.90"),
            CategoryId.newId(), null, true,
            List.of(new CreateProductCommand.VariationDraft("M", Money.brl("0.00")),
                    new CreateProductCommand.VariationDraft("G", Money.brl("10.00"))),
            List.of(new CreateProductCommand.AddOnGroupDraft("Adicionais", 0, 3,
                List.of(new CreateProductCommand.AddOnItemDraft("Bacon", Money.brl("3.00"))))));

        Result<ProductId> r = new CreateProductUseCase(products, categories).execute(cmd);
        assertThat(r.isSuccess()).isTrue();
        verify(products).save(any(Product.class));
    }

    @Test
    void rejectsCreateForUnknownCategory() {
        when(categories.existsById(any())).thenReturn(false);
        var cmd = new CreateProductCommand("X", "y", Money.brl("1.00"),
            CategoryId.newId(), null, false, List.of(), List.of());
        Result<ProductId> r = new CreateProductUseCase(products, categories).execute(cmd);
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    void togglesAvailability() {
        ProductId id = ProductId.newId();
        Product p = Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false);
        when(products.findById(id)).thenReturn(Optional.of(p));

        new SetProductAvailabilityUseCase(products).execute(new SetProductAvailabilityCommand(id, false));
        assertThat(p.isAvailable()).isFalse();
        verify(products).save(p);
    }

    @Test
    void setStockToTracked() {
        ProductId id = ProductId.newId();
        Product p = Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false);
        when(products.findById(id)).thenReturn(Optional.of(p));

        new SetProductStockUseCase(products).execute(new SetProductStockCommand(id, 25));
        assertThat(p.stock().quantity()).isEqualTo(25);
    }

    @Test
    void setStockToUntracked() {
        ProductId id = ProductId.newId();
        Product p = Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false);
        p.changeStock(Stock.of(10));
        when(products.findById(id)).thenReturn(Optional.of(p));

        new SetProductStockUseCase(products).execute(new SetProductStockCommand(id, null));
        assertThat(p.stock().isTracked()).isFalse();
    }

    @Test
    void deleteExistingProduct() {
        ProductId id = ProductId.newId();
        when(products.findById(id)).thenReturn(Optional.of(
            Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false)));
        Result<Void> r = new DeleteProductUseCase(products).execute(id);
        assertThat(r.isSuccess()).isTrue();
        verify(products).deleteById(id);
    }
}
