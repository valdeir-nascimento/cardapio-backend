package com.cardapio.catalog.api.rest;

import com.cardapio.catalog.api.dto.CategoryRequest;
import com.cardapio.catalog.api.dto.CategoryResponse;
import com.cardapio.catalog.application.CatalogFacade;
import com.cardapio.catalog.application.command.CreateCategoryCommand;
import com.cardapio.catalog.application.command.UpdateCategoryCommand;
import com.cardapio.catalog.domain.model.CategoryId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class CategoryAdminController implements CategoryAdminApi {

    private final CatalogFacade catalog;

    @Override
    @GetMapping
    public List<CategoryResponse> list() {
        return catalog.listCategories().stream()
            .map(c -> new CategoryResponse(c.id().value(), c.name(), c.displayOrder(), c.active()))
            .toList();
    }

    @Override
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid CategoryRequest req) {
        CategoryId id = catalog.createCategory(new CreateCategoryCommand(req.name(), req.displayOrder()));
        return ResponseEntity
            .created(URI.create("/api/v1/admin/categories/" + id.value()))
            .body(new CategoryResponse(id.value(), req.name(), req.displayOrder(), true));
    }

    @Override
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable UUID id, @Valid CategoryRequest req) {
        boolean active = Objects.requireNonNullElse(req.active(), true);
        catalog.updateCategory(new UpdateCategoryCommand(CategoryId.of(id), req.name(), req.displayOrder(), active));
        return new CategoryResponse(id, req.name(), req.displayOrder(), active);
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        catalog.deleteCategory(CategoryId.of(id));
        return ResponseEntity.noContent().build();
    }
}
