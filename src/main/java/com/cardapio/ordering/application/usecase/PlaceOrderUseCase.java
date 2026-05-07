package com.cardapio.ordering.application.usecase;

import com.cardapio.ordering.application.command.PlaceOrderCommand;
import com.cardapio.ordering.application.dto.PlacedOrderView;
import com.cardapio.ordering.domain.event.OrderPlaced;
import com.cardapio.ordering.domain.exception.DineInInvariantException;
import com.cardapio.ordering.domain.model.Cart;
import com.cardapio.ordering.domain.model.Comanda;
import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.ComandaStatus;
import com.cardapio.ordering.domain.model.DeliveryAddress;
import com.cardapio.ordering.domain.model.Order;
import com.cardapio.ordering.domain.model.OrderItem;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.TableId;
import com.cardapio.ordering.application.dto.CouponPricing;
import com.cardapio.ordering.domain.port.CartRepository;
import com.cardapio.ordering.domain.port.ComandaRepository;
import com.cardapio.ordering.domain.port.CouponPricingPort;
import com.cardapio.ordering.domain.port.DeliveryFeeQueryPort;
import com.cardapio.ordering.domain.port.IdempotencyKeyStore;
import com.cardapio.ordering.domain.port.OrderRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Money;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlaceOrderUseCase {

    private final CartRepository carts;
    private final OrderRepository orders;
    private final CartPricingService pricing;
    private final DeliveryFeeQueryPort deliveryFees;
    private final IdempotencyKeyStore idempotency;
    private final ComandaRepository comandas;
    private final CouponPricingPort couponPricing;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public Result<PlacedOrderView> execute(PlaceOrderCommand cmd) {
        if (cmd.idempotencyKey() == null || cmd.idempotencyKey().isBlank()) {
            return Result.failWith(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        // 1. Idempotency short-circuit
        Optional<com.cardapio.ordering.domain.model.OrderId> existing =
            idempotency.findExisting(cmd.idempotencyKey(), cmd.customerId());
        if (existing.isPresent()) {
            Order existingOrder = orders.findById(existing.get())
                .orElseThrow(() -> new IllegalStateException("idempotency points to missing order"));
            return Result.success(toPlacedView(existingOrder));
        }

        // 2. Load cart
        Cart cart = carts.findByCustomerId(cmd.customerId()).orElse(null);
        if (cart == null || cart.isEmpty()) {
            return Result.failWith(ErrorCode.CART_EMPTY);
        }

        // 3. Price + validate items against catalog
        Result<List<OrderItem>> priced = pricing.price(cart);
        if (!priced.isSuccess()) {
            return Result.failure(((Result.Failure<List<OrderItem>>) priced).notification());
        }
        List<OrderItem> items = ((Result.Success<List<OrderItem>>) priced).value();

        // 4. Compute delivery fee + address + dine-in context
        Currency currency = items.get(0).lineTotal().currency();
        Money fee = Money.of(BigDecimal.ZERO, currency);
        Optional<DeliveryAddress> address = Optional.empty();
        Optional<TableId> tableId = Optional.empty();
        Optional<ComandaId> comandaId = Optional.empty();
        Comanda dineInComanda = null;

        if (cmd.modality() == OrderModality.DELIVERY) {
            if (cmd.address() == null) return Result.failWith(ErrorCode.ADDRESS_REQUIRED);
            Optional<Money> feeOpt = deliveryFees.getActiveFee(cmd.address().neighborhoodId());
            if (feeOpt.isEmpty()) return Result.failWith(ErrorCode.NEIGHBORHOOD_NOT_SERVED);
            fee = feeOpt.get();
            try {
                address = Optional.of(toDeliveryAddress(cmd.address()));
            } catch (IllegalArgumentException e) {
                Notification n = Notification.empty();
                n.addError("address", ErrorCode.INVALID_INPUT, e.getMessage());
                return Result.failure(n);
            }
        } else if (cmd.modality() == OrderModality.DINE_IN) {
            if (cmd.tableId() == null || cmd.comandaId() == null) {
                return Result.failWith(ErrorCode.DINE_IN_CONTEXT_REQUIRED);
            }
            dineInComanda = comandas.findById(cmd.comandaId()).orElse(null);
            if (dineInComanda == null) return Result.failWith(ErrorCode.COMANDA_NOT_FOUND);
            if (dineInComanda.status() == ComandaStatus.CLOSED) return Result.failWith(ErrorCode.COMANDA_CLOSED);
            if (!dineInComanda.tableId().equals(cmd.tableId())) {
                return Result.failWith(ErrorCode.DINE_IN_CONTEXT_REQUIRED, "comanda does not belong to this table");
            }
            if (!dineInComanda.hasMember(cmd.customerId())) {
                return Result.failWith(ErrorCode.COMANDA_NOT_MEMBER);
            }
            tableId = Optional.of(cmd.tableId());
            comandaId = Optional.of(cmd.comandaId());
        }

        // 5. Evaluate coupon (re-check at checkout — may have expired since apply)
        Money subtotal = items.stream().map(OrderItem::lineTotal)
            .reduce(Money.of(BigDecimal.ZERO, currency), Money::add);
        Money discount = Money.of(BigDecimal.ZERO, currency);
        Optional<String> appliedCouponCode = Optional.empty();
        if (cart.couponCode().isPresent()) {
            CouponPricing pricing = couponPricing.evaluate(cart.couponCode().get(), subtotal);
            Result<Void> mapped = mapCouponPricing(pricing);
            if (!mapped.isSuccess()) {
                cart.removeCoupon(clock);
                carts.save(cart);
                return Result.failure(((Result.Failure<Void>) mapped).notification());
            }
            CouponPricing.Applicable applicable = (CouponPricing.Applicable) pricing;
            discount = applicable.discount();
            appliedCouponCode = Optional.of(applicable.code());
        }

        // 6. Build Order
        Order order;
        try {
            order = Order.place(cmd.customerId(), cmd.modality(), items, fee, address,
                tableId, comandaId, discount, appliedCouponCode, clock);
        } catch (IllegalArgumentException | DineInInvariantException e) {
            Notification n = Notification.empty();
            n.addError("order", ErrorCode.INVALID_INPUT, e.getMessage());
            return Result.failure(n);
        }

        // 7. Persist + idempotency (same TX)
        orders.save(order);
        idempotency.register(cmd.idempotencyKey(), cmd.customerId(), order.id());

        // 6b. Attach the order to its comanda (DINE_IN only)
        if (dineInComanda != null) {
            dineInComanda.attachOrder(order.id(), clock);
            comandas.save(dineInComanda);
        }

        // 7. Empty cart
        cart.clear(clock);
        carts.save(cart);

        // 8. Publish event (transactional outbox via Modulith)
        events.publishEvent(OrderPlaced.of(
            order.id(), order.customerId(), order.modality(), order.total(),
            order.appliedCouponCode().orElse(null), order.placedAt()));

        return Result.success(toPlacedView(order));
    }

    private static Result<Void> mapCouponPricing(CouponPricing pricing) {
        return switch (pricing) {
            case CouponPricing.Applicable a -> Result.ok();
            case CouponPricing.NotFound nf -> Result.failWith(ErrorCode.COUPON_NOT_FOUND);
            case CouponPricing.Inactive i -> Result.failWith(ErrorCode.COUPON_INACTIVE);
            case CouponPricing.Expired e -> Result.failWith(ErrorCode.COUPON_EXPIRED);
            case CouponPricing.Exhausted ex -> Result.failWith(ErrorCode.COUPON_EXHAUSTED);
            case CouponPricing.BelowMinOrder bm -> Result.failWith(ErrorCode.COUPON_BELOW_MIN_ORDER,
                "subtotal %s < minOrder %s".formatted(bm.subtotal().amount(), bm.minOrder().amount()));
        };
    }

    private DeliveryAddress toDeliveryAddress(PlaceOrderCommand.DeliveryAddressInput a) {
        return new DeliveryAddress(a.street(), a.number(), a.complement(),
            a.district(), a.city(), a.postalCode(), a.neighborhoodId());
    }

    private PlacedOrderView toPlacedView(Order order) {
        return new PlacedOrderView(
            order.id(), order.status(), order.modality(),
            order.total().amount(), order.total().currency().getCurrencyCode(),
            order.placedAt()
        );
    }
}
