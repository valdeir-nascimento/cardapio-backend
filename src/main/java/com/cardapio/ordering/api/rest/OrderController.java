package com.cardapio.ordering.api.rest;

import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.ordering.api.dto.OrderResponse;
import com.cardapio.ordering.api.dto.OrderSummaryResponse;
import com.cardapio.ordering.api.dto.PlaceOrderRequest;
import com.cardapio.ordering.api.dto.PlaceOrderResponse;
import com.cardapio.ordering.application.OrderingFacade;
import com.cardapio.ordering.application.dto.PlacedOrderView;
import com.cardapio.ordering.domain.model.OrderId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Validated
public class OrderController implements OrderApi {

    private final OrderingFacade ordering;

    @Override
    @PostMapping("/api/v1/orders")
    public ResponseEntity<PlaceOrderResponse> place(
        @AuthenticationPrincipal CardapioPrincipal me,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        PlaceOrderRequest body
    ) {
        PlacedOrderView view = ordering.placeOrder(body.toCommand(me.subject(), idempotencyKey));
        return ResponseEntity.created(URI.create("/api/v1/orders/" + view.id().value()))
            .body(PlaceOrderResponse.from(view));
    }

    @Override
    @GetMapping("/api/v1/orders/{id}")
    public OrderResponse get(@AuthenticationPrincipal CardapioPrincipal me, @PathVariable UUID id) {
        return OrderResponse.from(ordering.getMyOrder(me.subject(), OrderId.of(id)));
    }

    @Override
    @GetMapping("/api/v1/me/orders")
    public List<OrderSummaryResponse> mine(
        @AuthenticationPrincipal CardapioPrincipal me,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        return ordering.listMyOrders(me.subject(), limit, offset).stream()
            .map(OrderSummaryResponse::from).toList();
    }
}
