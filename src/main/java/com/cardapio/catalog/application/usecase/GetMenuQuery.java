package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.dto.CategoryView;
import com.cardapio.catalog.application.dto.MenuView;
import com.cardapio.catalog.application.dto.ProductSummaryView;
import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GetMenuQuery {

    private final CategoryRepository categories;
    private final ProductRepository products;

    @Transactional(readOnly = true)
    public MenuView execute() {
        List<Category> active = categories.findAllActive();
        List<CategoryId> ids = active.stream().map(Category::id).toList();
        Map<CategoryId, List<Product>> byCategory = products.findAvailableGroupedByCategories(ids);

        List<CategoryView> views = active.stream()
            .map(c -> {
                List<ProductSummaryView> productViews = byCategory
                    .getOrDefault(c.id(), List.of()).stream()
                    .map(p -> new ProductSummaryView(p.id(), p.name(), p.description(), p.basePrice(), p.imageUrl()))
                    .toList();
                return new CategoryView(c.id(), c.name(), c.displayOrder(), productViews);
            })
            .toList();

        return new MenuView(views);
    }
}
