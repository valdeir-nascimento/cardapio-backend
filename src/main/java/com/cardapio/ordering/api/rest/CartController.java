package com.cardapio.ordering.api.rest;

import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.ordering.api.dto.AddCartItemRequest;
import com.cardapio.ordering.api.dto.ApplyCouponRequest;
import com.cardapio.ordering.api.dto.CartResponse;
import com.cardapio.ordering.api.dto.UpdateCartItemRequest;
import com.cardapio.ordering.application.OrderingFacade;
import com.cardapio.ordering.application.command.RemoveCartItemCommand;
import com.cardapio.ordering.application.command.UpdateCartItemCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController implements CartApi {

    private final OrderingFacade ordering;

    @Override
    @GetMapping
    public CartResponse get(@AuthenticationPrincipal CardapioPrincipal me) {
        return CartResponse.from(ordering.getMyCart(me.subject()));
    }

    @Override
    @PostMapping("/items")
    public ResponseEntity<CartItemCreatedResponse> add(@AuthenticationPrincipal CardapioPrincipal me,
                                                       AddCartItemRequest body) {
        UUID id = ordering.addCartItem(body.toCommand(me.subject()));
        return ResponseEntity.created(URI.create("/api/v1/cart/items/" + id))
            .body(new CartItemCreatedResponse(id));
    }

    @Override
    @PutMapping("/items/{itemId}")
    public ResponseEntity<Void> update(@AuthenticationPrincipal CardapioPrincipal me, @PathVariable UUID itemId,
                                       UpdateCartItemRequest body) {
        ordering.updateCartItem(new UpdateCartItemCommand(me.subject(), itemId, body.quantity(), body.observation()));
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal CardapioPrincipal me, @PathVariable UUID itemId) {
        ordering.removeCartItem(new RemoveCartItemCommand(me.subject(), itemId));
    }

    @Override
    @PatchMapping("/coupon")
    public CartResponse applyCoupon(@AuthenticationPrincipal CardapioPrincipal me,
                                    ApplyCouponRequest body) {
        return CartResponse.from(ordering.applyCoupon(me.subject(), body.code()));
    }

    @Override
    @DeleteMapping("/coupon")
    public CartResponse removeCoupon(@AuthenticationPrincipal CardapioPrincipal me) {
        return CartResponse.from(ordering.removeCoupon(me.subject()));
    }

    public record CartItemCreatedResponse(UUID id) {}
}
