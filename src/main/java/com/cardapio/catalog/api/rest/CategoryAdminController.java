package com.cardapio.catalog.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.catalog.api.dto.CategoryRequest;
import com.cardapio.catalog.api.dto.CategoryResponse;
import com.cardapio.catalog.application.command.CreateCategoryCommand;
import com.cardapio.catalog.application.command.UpdateCategoryCommand;
import com.cardapio.catalog.application.usecase.CreateCategoryUseCase;
import com.cardapio.catalog.application.usecase.DeleteCategoryUseCase;
import com.cardapio.catalog.application.usecase.UpdateCategoryUseCase;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.shared.domain.Result;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class CategoryAdminController {

    private final CategoryRepository repo;
    private final CreateCategoryUseCase create;
    private final UpdateCategoryUseCase update;
    private final DeleteCategoryUseCase delete;

    public CategoryAdminController(CategoryRepository repo, CreateCategoryUseCase create,
                                   UpdateCategoryUseCase update, DeleteCategoryUseCase delete) {
        this.repo = repo; this.create = create; this.update = update; this.delete = delete;
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return repo.findAll().stream()
            .map(c -> new CategoryResponse(c.id().value(), c.name(), c.displayOrder(), c.isActive()))
            .toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CategoryRequest req) {
        Result<CategoryId> r = create.execute(new CreateCategoryCommand(req.name(), req.displayOrder()));
        return switch (r) {
            case Result.Success<CategoryId> s -> ResponseEntity.created(URI.create("/api/v1/admin/categories/" + s.value().value()))
                .body(new CategoryResponse(s.value().value(), req.name(), req.displayOrder(), true));
            case Result.Failure<CategoryId> f -> unprocessable(f);
        };
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest req) {
        boolean active = req.active() == null ? true : req.active();
        Result<CategoryId> r = update.execute(new UpdateCategoryCommand(CategoryId.of(id), req.name(), req.displayOrder(), active));
        return switch (r) {
            case Result.Success<CategoryId> s -> ResponseEntity.ok(new CategoryResponse(id, req.name(), req.displayOrder(), active));
            case Result.Failure<CategoryId> f -> unprocessable(f);
        };
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        Result<Void> r = delete.execute(CategoryId.of(id));
        return switch (r) {
            case Result.Success<Void> s -> ResponseEntity.noContent().build();
            case Result.Failure<Void> f -> unprocessable(f);
        };
    }

    private ResponseEntity<ProblemDetail> unprocessable(Result.Failure<?> f) {
        return ResponseEntity.unprocessableEntity()
            .contentType(MediaType.parseMediaType("application/problem+json"))
            .body(ProblemDetails.fromNotification(f.notification()));
    }
}
