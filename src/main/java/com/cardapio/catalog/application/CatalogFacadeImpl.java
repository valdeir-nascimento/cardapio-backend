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
import com.cardapio.catalog.application.usecase.CreateCategoryUseCase;
import com.cardapio.catalog.application.usecase.CreateProductUseCase;
import com.cardapio.catalog.application.usecase.DeleteCategoryUseCase;
import com.cardapio.catalog.application.usecase.DeleteProductUseCase;
import com.cardapio.catalog.application.usecase.GetMenuQuery;
import com.cardapio.catalog.application.usecase.GetOperatingHoursQuery;
import com.cardapio.catalog.application.usecase.GetProductDetailsQuery;
import com.cardapio.catalog.application.usecase.ListCategoriesQuery;
import com.cardapio.catalog.application.usecase.SetProductAvailabilityUseCase;
import com.cardapio.catalog.application.usecase.SetProductStockUseCase;
import com.cardapio.catalog.application.usecase.UpdateCategoryUseCase;
import com.cardapio.catalog.application.usecase.UpdateOperatingHoursUseCase;
import com.cardapio.catalog.application.usecase.UpdateProductUseCase;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogFacadeImpl implements CatalogFacade {

    private final CreateProductUseCase createProduct;
    private final UpdateProductUseCase updateProduct;
    private final DeleteProductUseCase deleteProduct;
    private final SetProductAvailabilityUseCase setAvailability;
    private final SetProductStockUseCase setStock;
    private final GetProductDetailsQuery getProductDetails;
    private final CreateCategoryUseCase createCategory;
    private final UpdateCategoryUseCase updateCategory;
    private final DeleteCategoryUseCase deleteCategory;
    private final ListCategoriesQuery listCategories;
    private final GetMenuQuery getMenu;
    private final GetOperatingHoursQuery getOperatingHours;
    private final UpdateOperatingHoursUseCase updateOperatingHours;

    @Override
    public ProductId createProduct(CreateProductCommand cmd) {
        return createProduct.execute(cmd).orElseThrow(NotificationException::new);
    }

    @Override
    public ProductId updateProduct(UpdateProductCommand cmd) {
        return updateProduct.execute(cmd).orElseThrow(NotificationException::new);
    }

    @Override
    public void deleteProduct(ProductId id) {
        deleteProduct.execute(id).orElseThrow(NotificationException::new);
    }

    @Override
    public void setProductAvailability(SetProductAvailabilityCommand cmd) {
        setAvailability.execute(cmd).orElseThrow(NotificationException::new);
    }

    @Override
    public void setProductStock(SetProductStockCommand cmd) {
        setStock.execute(cmd).orElseThrow(NotificationException::new);
    }

    @Override
    public ProductDetailsView getProductDetails(ProductId id) {
        return getProductDetails.execute(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public CategoryId createCategory(CreateCategoryCommand cmd) {
        return createCategory.execute(cmd).orElseThrow(NotificationException::new);
    }

    @Override
    public CategoryId updateCategory(UpdateCategoryCommand cmd) {
        return updateCategory.execute(cmd).orElseThrow(NotificationException::new);
    }

    @Override
    public void deleteCategory(CategoryId id) {
        deleteCategory.execute(id).orElseThrow(NotificationException::new);
    }

    @Override
    public List<CategorySummaryView> listCategories() {
        return listCategories.execute();
    }

    @Override
    public MenuView getMenu() {
        return getMenu.execute();
    }

    @Override
    public OperatingHoursView getOperatingHours() {
        return getOperatingHours.execute();
    }

    @Override
    public void updateOperatingHours(UpdateOperatingHoursCommand cmd) {
        updateOperatingHours.execute(cmd).orElseThrow(NotificationException::new);
    }
}
