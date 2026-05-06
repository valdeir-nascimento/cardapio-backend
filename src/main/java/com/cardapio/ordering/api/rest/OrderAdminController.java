package com.cardapio.ordering.api.rest;

import com.cardapio.ordering.api.dto.AdvanceStatusRequest;
import com.cardapio.ordering.api.dto.OrderResponse;
import com.cardapio.ordering.api.dto.OrderSummaryResponse;
import com.cardapio.ordering.application.OrderingFacade;
import com.cardapio.ordering.application.command.AdvanceOrderStatusCommand;
import com.cardapio.ordering.application.command.CancelOrderCommand;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasAnyRole('OWNER','MANAGER','OPERATOR')")
public class OrderAdminController {

    private final OrderingFacade ordering;

    @GetMapping
    public List<OrderSummaryResponse> list(
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        return ordering.listOrdersAdmin(status, from, to, limit, offset).stream()
            .map(OrderSummaryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return OrderResponse.from(ordering.getOrderAdmin(OrderId.of(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> advance(@PathVariable UUID id, @Valid @RequestBody AdvanceStatusRequest body) {
        ordering.advanceStatus(new AdvanceOrderStatusCommand(OrderId.of(id), body.status()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        ordering.cancelOrder(new CancelOrderCommand(OrderId.of(id)));
        return ResponseEntity.noContent().build();
    }
}
