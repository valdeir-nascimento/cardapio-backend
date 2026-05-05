package com.cardapio.catalog.api.rest;

import com.cardapio.catalog.api.assembler.ProductCommandAssembler;
import com.cardapio.catalog.api.dto.ProductRequest;
import com.cardapio.catalog.api.dto.SetAvailabilityRequest;
import com.cardapio.catalog.api.dto.SetStockRequest;
import com.cardapio.catalog.application.CatalogFacade;
import com.cardapio.catalog.application.command.SetProductAvailabilityCommand;
import com.cardapio.catalog.application.command.SetProductStockCommand;
import com.cardapio.catalog.domain.model.ProductId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
@RequiredArgsConstructor
public class ProductAdminController {

    private final CatalogFacade catalog;
    private final ProductCommandAssembler assembler;

    @PostMapping
    public ResponseEntity<Map<String, UUID>> create(@Valid @RequestBody ProductRequest req) {
        ProductId id = catalog.createProduct(assembler.toCreateCommand(req));
        return ResponseEntity
            .created(URI.create("/api/v1/admin/products/" + id.value()))
            .body(Map.of("id", id.value()));
    }

    @PutMapping("/{id}")
    public Map<String, UUID> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest req) {
        catalog.updateProduct(assembler.toUpdateCommand(id, req));
        return Map.of("id", id);
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<Void> availability(@PathVariable UUID id, @RequestBody SetAvailabilityRequest req) {
        catalog.setProductAvailability(new SetProductAvailabilityCommand(ProductId.of(id), req.available()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Void> stock(@PathVariable UUID id, @RequestBody SetStockRequest req) {
        catalog.setProductStock(new SetProductStockCommand(ProductId.of(id), req.quantity()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        catalog.deleteProduct(ProductId.of(id));
        return ResponseEntity.noContent().build();
    }
}
