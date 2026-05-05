// GetMenuQuery.java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.dto.CategoryView;
import com.cardapio.catalog.application.dto.MenuView;
import com.cardapio.catalog.application.dto.ProductSummaryView;
import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetMenuQuery {
    private final CategoryRepository categories;
    private final ProductRepository products;

    public GetMenuQuery(CategoryRepository categories, ProductRepository products) {
        this.categories = categories; this.products = products;
    }

    @Transactional(readOnly = true)
    public MenuView execute() {
        List<Category> active = categories.findAllActive();
        List<CategoryView> views = active.stream().map(c -> {
            List<Product> ps = products.findAvailableByCategory(c.id());
            List<ProductSummaryView> productViews = ps.stream()
                .map(p -> new ProductSummaryView(p.id(), p.name(), p.description(), p.basePrice(), p.imageUrl()))
                .toList();
            return new CategoryView(c.id(), c.name(), c.displayOrder(), productViews);
        }).toList();
        return new MenuView(views);
    }
}
