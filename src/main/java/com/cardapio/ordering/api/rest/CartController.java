package com.cardapio.ordering.api.rest;

import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.ordering.api.dto.AddCartItemRequest;
import com.cardapio.ordering.api.dto.CartResponse;
import com.cardapio.ordering.api.dto.UpdateCartItemRequest;
import com.cardapio.ordering.application.OrderingFacade;
import com.cardapio.ordering.application.command.RemoveCartItemCommand;
import com.cardapio.ordering.application.command.UpdateCartItemCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final OrderingFacade ordering;

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal CardapioPrincipal me) {
        return CartResponse.from(ordering.getMyCart(me.subject()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartItemCreatedResponse> add(@AuthenticationPrincipal CardapioPrincipal me,
                                                       @Valid @RequestBody AddCartItemRequest body) {
        UUID id = ordering.addCartItem(body.toCommand(me.subject()));
        return ResponseEntity.created(URI.create("/api/v1/cart/items/" + id))
            .body(new CartItemCreatedResponse(id));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<Void> update(@AuthenticationPrincipal CardapioPrincipal me, @PathVariable UUID itemId,
                                       @Valid @RequestBody UpdateCartItemRequest body) {
        ordering.updateCartItem(new UpdateCartItemCommand(me.subject(), itemId, body.quantity(), body.observation()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal CardapioPrincipal me, @PathVariable UUID itemId) {
        ordering.removeCartItem(new RemoveCartItemCommand(me.subject(), itemId));
    }

    public record CartItemCreatedResponse(UUID id) {}
}
