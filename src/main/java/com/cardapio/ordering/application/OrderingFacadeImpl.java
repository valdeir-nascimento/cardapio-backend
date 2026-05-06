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
import com.cardapio.ordering.application.usecase.AddCartItemUseCase;
import com.cardapio.ordering.application.usecase.AdvanceOrderStatusUseCase;
import com.cardapio.ordering.application.usecase.CancelOrderUseCase;
import com.cardapio.ordering.application.usecase.GetCartQuery;
import com.cardapio.ordering.application.usecase.GetOrderQuery;
import com.cardapio.ordering.application.usecase.ListOrdersQuery;
import com.cardapio.ordering.application.usecase.PlaceOrderUseCase;
import com.cardapio.ordering.application.usecase.RemoveCartItemUseCase;
import com.cardapio.ordering.application.usecase.UpdateCartItemUseCase;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.shared.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderingFacadeImpl implements OrderingFacade {

    private final AddCartItemUseCase addCartItem;
    private final UpdateCartItemUseCase updateCartItem;
    private final RemoveCartItemUseCase removeCartItem;
    private final GetCartQuery getCart;
    private final PlaceOrderUseCase placeOrder;
    private final GetOrderQuery getOrder;
    private final ListOrdersQuery listOrders;
    private final AdvanceOrderStatusUseCase advanceStatus;
    private final CancelOrderUseCase cancelOrder;

    @Override
    public UUID addCartItem(AddCartItemCommand cmd) {
        return addCartItem.execute(cmd).orElseThrow(OrderingFacadeImpl::toException);
    }

    @Override
    public void updateCartItem(UpdateCartItemCommand cmd) {
        updateCartItem.execute(cmd).orElseThrow(OrderingFacadeImpl::toException);
    }

    @Override
    public void removeCartItem(RemoveCartItemCommand cmd) {
        removeCartItem.execute(cmd).orElseThrow(OrderingFacadeImpl::toException);
    }

    @Override
    public CartView getMyCart(UUID customerId) {
        return getCart.getOrEmpty(customerId);
    }

    @Override
    public PlacedOrderView placeOrder(PlaceOrderCommand cmd) {
        return placeOrder.execute(cmd).orElseThrow(OrderingFacadeImpl::toException);
    }

    @Override
    public OrderView getMyOrder(UUID customerId, OrderId orderId) {
        return getOrder.getForCustomer(orderId, customerId)
            .orElseThrow(() -> new NotFoundException(
                Notification.ofSingle(com.cardapio.shared.domain.ErrorCode.ORDER_NOT_FOUND)));
    }

    @Override
    public List<OrderSummaryView> listMyOrders(UUID customerId, int limit, int offset) {
        return listOrders.listMyOrders(customerId, limit, offset);
    }

    @Override
    public OrderView getOrderAdmin(OrderId orderId) {
        return getOrder.getAdmin(orderId)
            .orElseThrow(() -> new NotFoundException(
                Notification.ofSingle(com.cardapio.shared.domain.ErrorCode.ORDER_NOT_FOUND)));
    }

    @Override
    public List<OrderSummaryView> listOrdersAdmin(OrderStatus status, Instant from, Instant to, int limit, int offset) {
        return listOrders.listAdmin(status, from, to, limit, offset);
    }

    @Override
    public void advanceStatus(AdvanceOrderStatusCommand cmd) {
        advanceStatus.execute(cmd).orElseThrow(OrderingFacadeImpl::toException);
    }

    @Override
    public void cancelOrder(CancelOrderCommand cmd) {
        cancelOrder.execute(cmd).orElseThrow(OrderingFacadeImpl::toException);
    }

    private static RuntimeException toException(Notification n) {
        boolean notFound = n.errors().stream()
            .anyMatch(e -> e.code() != null && e.code().endsWith("_NOT_FOUND"));
        return notFound ? new NotFoundException(n) : new NotificationException(n);
    }
}
