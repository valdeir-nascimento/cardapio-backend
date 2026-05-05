package com.cardapio.catalog.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.catalog.api.dto.*;
import com.cardapio.catalog.application.command.*;
import com.cardapio.catalog.application.usecase.*;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.shared.domain.Money;
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
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class ProductAdminController {

    private final CreateProductUseCase create;
    private final UpdateProductUseCase update;
    private final DeleteProductUseCase delete;
    private final SetProductAvailabilityUseCase setAvailability;
    private final SetProductStockUseCase setStock;

    public ProductAdminController(CreateProductUseCase create, UpdateProductUseCase update, DeleteProductUseCase delete,
                                  SetProductAvailabilityUseCase setAvailability, SetProductStockUseCase setStock) {
        this.create = create; this.update = update; this.delete = delete;
        this.setAvailability = setAvailability; this.setStock = setStock;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ProductRequest req) {
        var cmd = toCreateCommand(req);
        Result<ProductId> r = create.execute(cmd);
        return switch (r) {
            case Result.Success<ProductId> s -> ResponseEntity.created(URI.create("/api/v1/admin/products/" + s.value().value()))
                .body(java.util.Map.of("id", s.value().value()));
            case Result.Failure<ProductId> f -> unprocessable(f);
        };
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest req) {
        var cmd = new UpdateProductCommand(
            ProductId.of(id), req.name(), req.description() == null ? "" : req.description(),
            Money.brl(req.basePrice().toPlainString()), CategoryId.of(req.categoryId()),
            req.imageUrl(), req.allowsHalfHalf(),
            mapVariations(req.variations()), mapAddOns(req.addOnGroups()));
        Result<ProductId> r = update.execute(cmd);
        return switch (r) {
            case Result.Success<ProductId> s -> ResponseEntity.ok(java.util.Map.of("id", id));
            case Result.Failure<ProductId> f -> unprocessable(f);
        };
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<?> availability(@PathVariable UUID id, @RequestBody SetAvailabilityRequest req) {
        Result<Void> r = setAvailability.execute(new SetProductAvailabilityCommand(ProductId.of(id), req.available()));
        return switch (r) {
            case Result.Success<Void> s -> ResponseEntity.noContent().build();
            case Result.Failure<Void> f -> unprocessable(f);
        };
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> stock(@PathVariable UUID id, @RequestBody SetStockRequest req) {
        Result<Void> r = setStock.execute(new SetProductStockCommand(ProductId.of(id), req.quantity()));
        return switch (r) {
            case Result.Success<Void> s -> ResponseEntity.noContent().build();
            case Result.Failure<Void> f -> unprocessable(f);
        };
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        Result<Void> r = delete.execute(ProductId.of(id));
        return switch (r) {
            case Result.Success<Void> s -> ResponseEntity.noContent().build();
            case Result.Failure<Void> f -> unprocessable(f);
        };
    }

    private CreateProductCommand toCreateCommand(ProductRequest req) {
        return new CreateProductCommand(req.name(), req.description() == null ? "" : req.description(),
            Money.brl(req.basePrice().toPlainString()), CategoryId.of(req.categoryId()),
            req.imageUrl(), req.allowsHalfHalf(),
            mapVariations(req.variations()), mapAddOns(req.addOnGroups()));
    }

    private List<CreateProductCommand.VariationDraft> mapVariations(List<VariationRequest> vs) {
        if (vs == null) return List.of();
        return vs.stream()
            .map(v -> new CreateProductCommand.VariationDraft(v.name(), Money.brl(v.priceModifier().toPlainString())))
            .toList();
    }

    private List<CreateProductCommand.AddOnGroupDraft> mapAddOns(List<AddOnGroupRequest> gs) {
        if (gs == null) return List.of();
        return gs.stream()
            .map(g -> new CreateProductCommand.AddOnGroupDraft(g.name(), g.minSelection(), g.maxSelection(),
                g.items().stream().map(i -> new CreateProductCommand.AddOnItemDraft(i.name(), Money.brl(i.price().toPlainString()))).toList()))
            .toList();
    }

    private ResponseEntity<ProblemDetail> unprocessable(Result.Failure<?> f) {
        return ResponseEntity.unprocessableEntity()
            .contentType(MediaType.parseMediaType("application/problem+json"))
            .body(ProblemDetails.fromNotification(f.notification()));
    }
}
