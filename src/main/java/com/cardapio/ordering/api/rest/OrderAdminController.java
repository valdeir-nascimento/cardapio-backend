package com.cardapio.ordering.api.rest;

import com.cardapio.ordering.api.dto.AdvanceStatusRequest;
import com.cardapio.ordering.api.dto.OrderResponse;
import com.cardapio.ordering.api.dto.OrderSummaryResponse;
import com.cardapio.ordering.application.OrderingFacade;
import com.cardapio.ordering.application.command.AdvanceOrderStatusCommand;
import com.cardapio.ordering.application.command.CancelOrderCommand;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasAnyRole('OWNER','MANAGER','OPERATOR')")
public class OrderAdminController implements OrderAdminApi {

    private final OrderingFacade ordering;

    @Override
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

    @Override
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        return OrderResponse.from(ordering.getOrderAdmin(OrderId.of(id)));
    }

    @Override
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> advance(@PathVariable UUID id, AdvanceStatusRequest body) {
        ordering.advanceStatus(new AdvanceOrderStatusCommand(OrderId.of(id), body.status()));
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        ordering.cancelOrder(new CancelOrderCommand(OrderId.of(id)));
        return ResponseEntity.noContent().build();
    }
}
