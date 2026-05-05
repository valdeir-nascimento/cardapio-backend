package com.cardapio.catalog.application;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.api.error.NotificationException;
import com.cardapio.catalog.application.command.CreateCategoryCommand;
import com.cardapio.catalog.application.command.CreateProductCommand;
import com.cardapio.catalog.application.command.SetProductAvailabilityCommand;
import com.cardapio.catalog.application.command.SetProductStockCommand;
import com.cardapio.catalog.application.command.UpdateCategoryCommand;
import com.cardapio.catalog.application.command.UpdateOperatingHoursCommand;
import com.cardapio.catalog.application.command.UpdateProductCommand;
import com.cardapio.catalog.application.dto.CategorySummaryView;
import com.cardapio.catalog.application.dto.MenuView;
import com.cardapio.catalog.application.dto.OperatingHoursView;
import com.cardapio.catalog.application.dto.ProductDetailsView;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.ProductId;

import java.util.List;

public interface CatalogFacade {

    // Product
    ProductId createProduct(CreateProductCommand cmd) throws NotificationException;
    ProductId updateProduct(UpdateProductCommand cmd) throws NotificationException;
    void deleteProduct(ProductId id) throws NotificationException;
    void setProductAvailability(SetProductAvailabilityCommand cmd) throws NotificationException;
    void setProductStock(SetProductStockCommand cmd) throws NotificationException;
    ProductDetailsView getProductDetails(ProductId id) throws NotFoundException;

    // Category
    CategoryId createCategory(CreateCategoryCommand cmd) throws NotificationException;
    CategoryId updateCategory(UpdateCategoryCommand cmd) throws NotificationException;
    void deleteCategory(CategoryId id) throws NotificationException;
    List<CategorySummaryView> listCategories();

    // Menu & Operating Hours
    MenuView getMenu();
    OperatingHoursView getOperatingHours();
    void updateOperatingHours(UpdateOperatingHoursCommand cmd) throws NotificationException;
}
