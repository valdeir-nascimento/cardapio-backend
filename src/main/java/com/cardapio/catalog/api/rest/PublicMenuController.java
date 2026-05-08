package com.cardapio.catalog.api.rest;

import com.cardapio.catalog.api.dto.MenuResponse;
import com.cardapio.catalog.api.dto.ProductDetailsResponse;
import com.cardapio.catalog.application.CatalogFacade;
import com.cardapio.catalog.domain.model.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/menu")
public class PublicMenuController implements PublicMenuApi {

    private final CatalogFacade catalog;

    @Override
    @GetMapping
    public MenuResponse menu() {
        return MenuResponse.from(catalog.getMenu());
    }

    @Override
    @GetMapping("/products/{id}")
    public ProductDetailsResponse product(@PathVariable UUID id) {
        return ProductDetailsResponse.from(catalog.getProductDetails(ProductId.of(id)));
    }
}
