package com.cardapio.catalog.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.catalog.api.dto.MenuResponse;
import com.cardapio.catalog.api.dto.ProductDetailsResponse;
import com.cardapio.catalog.application.dto.ProductDetailsView;
import com.cardapio.catalog.application.usecase.GetMenuQuery;
import com.cardapio.catalog.application.usecase.GetProductDetailsQuery;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.shared.domain.Result;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menu")
public class PublicMenuController {

    private final GetMenuQuery menu;
    private final GetProductDetailsQuery details;

    public PublicMenuController(GetMenuQuery menu, GetProductDetailsQuery details) {
        this.menu = menu; this.details = details;
    }

    @GetMapping
    public MenuResponse menu() {
        return MenuResponse.from(menu.execute());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> product(@PathVariable UUID id) {
        Result<ProductDetailsView> r = details.execute(ProductId.of(id));
        return switch (r) {
            case Result.Success<ProductDetailsView> s -> ResponseEntity.ok(ProductDetailsResponse.from(s.value()));
            case Result.Failure<ProductDetailsView> f -> ResponseEntity.status(404)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetails.fromNotification(f.notification()));
        };
    }
}
