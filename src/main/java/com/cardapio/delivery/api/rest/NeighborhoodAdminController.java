package com.cardapio.delivery.api.rest;

import com.cardapio.delivery.api.dto.NeighborhoodRequest;
import com.cardapio.delivery.api.dto.NeighborhoodResponse;
import com.cardapio.delivery.application.DeliveryFacade;
import com.cardapio.delivery.application.command.CreateNeighborhoodCommand;
import com.cardapio.delivery.application.command.UpdateNeighborhoodCommand;
import com.cardapio.delivery.domain.model.NeighborhoodId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/neighborhoods")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class NeighborhoodAdminController {

    private final DeliveryFacade delivery;

    @GetMapping
    public List<NeighborhoodResponse> list() {
        return delivery.listAll().stream().map(NeighborhoodResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<NeighborhoodResponse> create(@Valid @RequestBody NeighborhoodRequest req) {
        NeighborhoodId id = delivery.createNeighborhood(
            new CreateNeighborhoodCommand(req.name(), req.city(), req.fee()));
        return ResponseEntity
            .created(URI.create("/api/v1/admin/neighborhoods/" + id.value()))
            .body(new NeighborhoodResponse(id.value(), req.name(), req.city(), req.fee(), "BRL", true));
    }

    @PutMapping("/{id}")
    public NeighborhoodResponse update(@PathVariable UUID id, @Valid @RequestBody NeighborhoodRequest req) {
        boolean active = Objects.requireNonNullElse(req.active(), true);
        delivery.updateNeighborhood(new UpdateNeighborhoodCommand(
            NeighborhoodId.of(id), req.name(), req.city(), req.fee(), active));
        return new NeighborhoodResponse(id, req.name(), req.city(), req.fee(), "BRL", active);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        delivery.deleteNeighborhood(NeighborhoodId.of(id));
        return ResponseEntity.noContent().build();
    }
}
