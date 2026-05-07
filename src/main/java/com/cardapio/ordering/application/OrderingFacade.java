package com.cardapio.ordering.application;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.api.error.NotificationException;
import com.cardapio.ordering.application.command.AddCartItemCommand;
import com.cardapio.ordering.application.command.AdvanceOrderStatusCommand;
import com.cardapio.ordering.application.command.CancelOrderCommand;
import com.cardapio.ordering.application.command.PlaceOrderCommand;
import com.cardapio.ordering.application.command.RemoveCartItemCommand;
import com.cardapio.ordering.application.command.UpdateCartItemCommand;
import com.cardapio.ordering.application.dto.CartView;
import com.cardapio.ordering.application.dto.OrderSummaryView;
import com.cardapio.ordering.application.dto.OrderView;
import com.cardapio.ordering.application.dto.PlacedOrderView;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderStatus;
import org.springframework.modulith.NamedInterface;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@NamedInterface("OrderingFacade")
public interface OrderingFacade {

    // Cart
    UUID addCartItem(AddCartItemCommand cmd) throws NotificationException;
    void updateCartItem(UpdateCartItemCommand cmd) throws NotificationException;
    void removeCartItem(RemoveCartItemCommand cmd) throws NotificationException;
    CartView getMyCart(UUID customerId);
    CartView applyCoupon(UUID customerId, String code) throws NotificationException;
    CartView removeCoupon(UUID customerId);

    // Orders — customer
    PlacedOrderView placeOrder(PlaceOrderCommand cmd) throws NotificationException;
    OrderView getMyOrder(UUID customerId, OrderId orderId) throws NotFoundException;
    List<OrderSummaryView> listMyOrders(UUID customerId, int limit, int offset);

    // Orders — admin
    OrderView getOrderAdmin(OrderId orderId) throws NotFoundException;
    List<OrderSummaryView> listOrdersAdmin(OrderStatus status, Instant from, Instant to, int limit, int offset);
    void advanceStatus(AdvanceOrderStatusCommand cmd) throws NotificationException;
    void cancelOrder(CancelOrderCommand cmd) throws NotificationException;

    /**
     * Returns distinct product IDs in the order's items, preserving line order.
     * Used by cross-context read flows (e.g. review projection) to avoid
     * carrying items on every domain event.
     */
    List<UUID> getOrderProductIds(OrderId orderId) throws NotFoundException;
}
