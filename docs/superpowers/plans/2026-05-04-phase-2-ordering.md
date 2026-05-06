# Phase 2 — Ordering (Cart + Order: Delivery + Pickup) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the `delivery` and `ordering` bounded contexts. `delivery` exposes a list of served neighborhoods with delivery fee. `ordering` is the heart of the system: customer-scoped `Cart` (held in DB), `Order` aggregate with frozen pricing snapshot and a status workflow for the `DELIVERY` and `PICKUP` modalities, idempotent checkout, admin status transition, and domain events `OrderPlaced` / `OrderStatusChanged` / `OrderCanceled` published via Spring Modulith outbox so future phases (payment, notification, stock) can subscribe.

**Architecture:** Clean Architecture inside `com.cardapio.delivery` and `com.cardapio.ordering`. Aggregates: `Neighborhood` (delivery), `Cart`, `Order` (ordering). `Cart` has many `CartItem`s (with chosen variation, add-on selections, half-and-half halves, free-text observation, qty). `Order` has many `OrderItem`s with **snapshot of price/name/options** at the moment of placement — once placed, catalog price changes don't move the order total. `Cart` is reconciled against `catalog` at checkout: prices, availability, and stock are re-fetched and revalidated. Use cases return `Result<T>`; queries return read-only views. `POST /cart/items` and admin status endpoints require role `OWNER`/`MANAGER`/`OPERATOR` for admin paths and customer JWT for customer paths.

**Tech Stack:** Reuses everything from Phase 1.A/1.B/1.C. No new dependencies — Spring Modulith `ApplicationEventPublisher` + Event Publication Registry are already on the classpath (used in Phase 1.A migration `V1__event_publication.sql`).

**Reference:**
- Spec: [docs/superpowers/specs/2026-05-04-cardapio-digital-backend-design.md](../specs/2026-05-04-cardapio-digital-backend-design.md) — §3 `ordering` & `delivery`, §4 carrinho/checkout/admin, §5 eventos, §7.2 Result + Notification
- Phase 1.B plan: [docs/superpowers/plans/2026-05-04-phase-1b-identity.md](./2026-05-04-phase-1b-identity.md) — JPA + JWT patterns
- Phase 1.C plan: [docs/superpowers/plans/2026-05-04-phase-1c-catalog.md](./2026-05-04-phase-1c-catalog.md) — module/use case/controller scaffolding patterns (referenced extensively)

**Out of scope (deferred to later phases):**
- Real payment integration (Mercado Pago) → Phase 3 — `OrderPlaced` is published, but `payment` module that consumes it is Phase 3
- Coupon application on cart → Phase 6 (`promotion`) — endpoints `POST /cart/coupon` and `DELETE /cart/coupon` are NOT in scope; `Order.discount` field exists but is always zero in this phase
- SSE streams (`/orders/{id}/status/stream`, `/admin/orders/stream`) → Phase 4 (notification)
- E-mail / WhatsApp customer notifications on status change → Phase 4
- Stock decrement on `OrderPlaced` → Phase 4 (or whenever catalog adds an event listener); for now stock is only validated on checkout, not decremented
- `DINE_IN` / table / comanda → Phase 5
- `POST /me/orders/{id}/repeat` and `POST /me/orders/{id}/review` → Phase 6

**Cross-context contract (key design decision):**
- `ordering` must read product/variation/add-on prices and check availability/stock. To keep the modules decoupled, `ordering` does **not** depend on `catalog` JPA — instead, `catalog` exposes a query port `CatalogQueryFacade` (a `@NamedInterface` published from the `catalog::application` package) that returns a `ProductDetailsView`. `ordering` calls this facade through its own port `CatalogQueryPort`, and a single adapter in `ordering.infrastructure` wires `CatalogQueryPort` → `CatalogFacade` (no new types needed in catalog: the existing `CatalogFacade.getProductDetails()` is reused).
- Cross-schema FKs are forbidden (Modulith rule). `orders.customer_id`, `orders.product_id`, `cart.customer_id` are plain UUIDs, not FKs.

---

## File Structure

```
src/main/java/com/cardapio/delivery/
├── domain/
│   ├── model/
│   │   ├── NeighborhoodId.java
│   │   └── Neighborhood.java
│   ├── port/
│   │   └── NeighborhoodRepository.java
│   └── exception/
│       └── NeighborhoodNotFoundException.java
├── application/
│   ├── DeliveryFacade.java
│   ├── DeliveryFacadeImpl.java
│   ├── command/
│   │   ├── CreateNeighborhoodCommand.java
│   │   └── UpdateNeighborhoodCommand.java
│   ├── usecase/
│   │   ├── CreateNeighborhoodUseCase.java
│   │   ├── UpdateNeighborhoodUseCase.java
│   │   ├── DeleteNeighborhoodUseCase.java
│   │   ├── ListNeighborhoodsQuery.java
│   │   └── GetDeliveryFeeQuery.java
│   └── dto/
│       └── NeighborhoodView.java
├── infrastructure/
│   └── persistence/
│       ├── jpa/NeighborhoodJpaEntity.java
│       ├── repository/SpringNeighborhoodJpaRepository.java
│       ├── mapper/NeighborhoodMapper.java
│       └── adapter/NeighborhoodRepositoryAdapter.java
├── api/
│   ├── rest/
│   │   ├── NeighborhoodAdminController.java
│   │   └── PublicDeliveryController.java
│   └── dto/
│       ├── NeighborhoodRequest.java
│       └── NeighborhoodResponse.java
└── package-info.java

src/main/java/com/cardapio/ordering/
├── domain/
│   ├── model/
│   │   ├── CartId.java + Cart.java + CartItem.java + CartItemId.java
│   │   ├── OrderId.java + Order.java + OrderItem.java + OrderItemId.java
│   │   ├── OrderStatus.java          (enum + transition rules)
│   │   ├── OrderModality.java        (enum: DELIVERY, PICKUP)
│   │   ├── DeliveryAddress.java      (VO)
│   │   ├── SelectedVariation.java    (VO: variationId + name + priceModifier — snapshot)
│   │   ├── SelectedAddOn.java        (VO: groupId, itemId, name, price — snapshot)
│   │   ├── HalfAndHalf.java          (VO: leftProductId, rightProductId, name, basePrice — snapshot)
│   │   └── Observation.java          (VO: trimmed text up to 200 chars)
│   ├── event/
│   │   ├── OrderPlaced.java
│   │   ├── OrderStatusChanged.java
│   │   └── OrderCanceled.java
│   ├── port/
│   │   ├── CartRepository.java
│   │   ├── OrderRepository.java
│   │   ├── CatalogQueryPort.java       (abstraction over catalog facade)
│   │   ├── DeliveryFeeQueryPort.java   (abstraction over delivery facade)
│   │   └── IdempotencyKeyStore.java
│   └── exception/
│       ├── CartNotFoundException.java
│       ├── OrderNotFoundException.java
│       └── IllegalStatusTransitionException.java
├── application/
│   ├── OrderingFacade.java + OrderingFacadeImpl.java
│   ├── command/
│   │   ├── AddCartItemCommand.java
│   │   ├── UpdateCartItemCommand.java
│   │   ├── RemoveCartItemCommand.java
│   │   ├── PlaceOrderCommand.java
│   │   ├── AdvanceOrderStatusCommand.java
│   │   └── CancelOrderCommand.java
│   ├── usecase/
│   │   ├── AddCartItemUseCase.java
│   │   ├── UpdateCartItemUseCase.java
│   │   ├── RemoveCartItemUseCase.java
│   │   ├── GetCartQuery.java
│   │   ├── PlaceOrderUseCase.java       (the big one — pricing, validation, snapshot, event)
│   │   ├── AdvanceOrderStatusUseCase.java
│   │   ├── CancelOrderUseCase.java
│   │   ├── GetOrderQuery.java
│   │   ├── ListMyOrdersQuery.java
│   │   ├── ListOrdersAdminQuery.java
│   │   └── CartPricingService.java       (helper: prices a Cart against catalog snapshot)
│   └── dto/
│       ├── CartView.java + CartItemView.java
│       ├── OrderView.java + OrderItemView.java + OrderSummaryView.java
│       └── PlacedOrderView.java
├── infrastructure/
│   ├── persistence/
│   │   ├── jpa/{CartJpaEntity, CartItemJpaEntity, OrderJpaEntity, OrderItemJpaEntity, IdempotencyKeyJpaEntity}.java
│   │   ├── repository/{SpringCartJpa, SpringOrderJpa, SpringIdempotencyJpa}Repository.java
│   │   ├── mapper/{CartMapper, OrderMapper}.java
│   │   └── adapter/{CartRepositoryAdapter, OrderRepositoryAdapter, JpaIdempotencyKeyStore}.java
│   └── catalog/
│       ├── CatalogQueryAdapter.java       (CatalogQueryPort → CatalogFacade)
│       └── DeliveryFeeQueryAdapter.java   (DeliveryFeeQueryPort → DeliveryFacade)
├── api/
│   ├── rest/
│   │   ├── CartController.java                    (customer-auth)
│   │   ├── OrderController.java                   (customer-auth)
│   │   └── OrderAdminController.java              (admin-auth)
│   └── dto/
│       ├── AddCartItemRequest.java + UpdateCartItemRequest.java
│       ├── CartResponse.java + CartItemResponse.java
│       ├── PlaceOrderRequest.java + PlaceOrderResponse.java
│       ├── OrderResponse.java + OrderSummaryResponse.java
│       └── AdvanceStatusRequest.java
└── package-info.java

src/main/resources/db/migration/
└── V6__delivery_and_ordering_tables.sql

src/main/java/com/cardapio/catalog/application/CatalogFacade.java         (ADD: `@NamedInterface` so ordering can depend on catalog::application)
src/main/java/com/cardapio/identity/api/security/SecurityConfig.java     (modify: add ordering paths)
```

---

## Task 1: Delivery module skeleton + Neighborhood domain

**Files:**
- Create: `src/main/java/com/cardapio/delivery/package-info.java`
- Create: `src/main/java/com/cardapio/delivery/domain/model/NeighborhoodId.java`
- Create: `src/main/java/com/cardapio/delivery/domain/model/Neighborhood.java`
- Test: `src/test/java/com/cardapio/delivery/domain/model/NeighborhoodTest.java`

- [ ] **Step 1.1:** package-info

```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Delivery",
    allowedDependencies = {"shared", "api::error", "api::support"}
)
package com.cardapio.delivery;
```

- [ ] **Step 1.2:** `NeighborhoodId` — same record-VO pattern as `CategoryId` (Phase 1.C Task 2). UUID inside, `newId()`, `of(UUID)`, `of(String)`.

- [ ] **Step 1.3:** `Neighborhood` aggregate — fields `id`, `name`, `city`, `Money fee`, `boolean active`. Static factories `create(name, city, fee)` and `rehydrate(id, name, city, fee, active)`. Mutators: `rename`, `relocate`, `changeFee`, `activate`, `deactivate`. Validate: name & city non-blank ≤ 120, fee ≥ 0 BRL.

- [ ] **Step 1.4:** Tests — `creates with active=true by default`, `rejects negative fee`, `rejects blank name/city`, `deactivate flips flag`.

- [ ] **Step 1.5:** Verify Modulith verifier still passes: `./mvnw test -Dtest=ModulithVerificationTest`.

- [ ] **Step 1.6:** Commit
```bash
git add src/main/java/com/cardapio/delivery/package-info.java src/main/java/com/cardapio/delivery/domain src/test/java/com/cardapio/delivery
git commit -m "feat(delivery): add module skeleton and Neighborhood aggregate"
```

---

## Task 2: Delivery — port, exception, repository

**Files:**
- `src/main/java/com/cardapio/delivery/domain/port/NeighborhoodRepository.java`
- `src/main/java/com/cardapio/delivery/domain/exception/NeighborhoodNotFoundException.java`

- [ ] **Step 2.1:** Repository port — `save(Neighborhood)`, `findById(NeighborhoodId)`, `findAllActive()`, `findAll()`, `deleteById(NeighborhoodId)`, `existsByNameAndCity(String, String)`.

- [ ] **Step 2.2:** `NeighborhoodNotFoundException extends NotFoundException` (same shape as `CategoryNotFoundException`).

- [ ] **Step 2.3:** Commit
```bash
git add src/main/java/com/cardapio/delivery/domain
git commit -m "feat(delivery): add Neighborhood repository port and exception"
```

---

## Task 3: Delivery use cases + facade

**Files:**
- 5 commands in `application/command/`
- 5 use cases in `application/usecase/`
- `application/dto/NeighborhoodView.java`
- `application/DeliveryFacade.java` + `DeliveryFacadeImpl.java`

- [ ] **Step 3.1:** Commands `CreateNeighborhoodCommand(name, city, BigDecimal fee)` and `UpdateNeighborhoodCommand(NeighborhoodId id, name, city, BigDecimal fee, boolean active)`.

- [ ] **Step 3.2:** `NeighborhoodView(UUID id, String name, String city, BigDecimal fee, boolean active)`.

- [ ] **Step 3.3:** Use cases mirror Phase 1.C `CategoryUseCases` shape. Validate via `Notification` and throw `NotificationException` from facade when failures exist (consistent with existing catalog facade).
  - `CreateNeighborhoodUseCase` — fails if `(name, city)` already exists.
  - `UpdateNeighborhoodUseCase` — `findById` → mutate → save; throws `NeighborhoodNotFoundException` if absent.
  - `DeleteNeighborhoodUseCase` — `existsById` then `deleteById`.
  - `ListNeighborhoodsQuery` — admin: returns all (active+inactive). Public path will call `findAllActive` instead.
  - `GetDeliveryFeeQuery(NeighborhoodId)` — returns `Money` fee; if not found or inactive → empty `Optional`. (Used by ordering.)

- [ ] **Step 3.4:** `DeliveryFacade` interface:
```java
public interface DeliveryFacade {
    NeighborhoodId createNeighborhood(CreateNeighborhoodCommand cmd) throws NotificationException;
    NeighborhoodId updateNeighborhood(UpdateNeighborhoodCommand cmd) throws NotificationException;
    void deleteNeighborhood(NeighborhoodId id) throws NotificationException;
    List<NeighborhoodView> listAll();
    List<NeighborhoodView> listActive();
    Optional<NeighborhoodView> getById(NeighborhoodId id);
}
```
Annotate with `@org.springframework.modulith.NamedInterface("DeliveryFacade")` so `ordering` can declare `delivery::DeliveryFacade` as an allowed dependency.

- [ ] **Step 3.5:** `DeliveryFacadeImpl` — `@Service`, constructor-injects the 5 use cases.

- [ ] **Step 3.6:** Unit tests for `CreateNeighborhoodUseCase` and `UpdateNeighborhoodUseCase` against a fake in-memory repository (mirroring `CategoryUseCasesTest` style).

- [ ] **Step 3.7:** Commit
```bash
git add src/main/java/com/cardapio/delivery/application src/test/java/com/cardapio/delivery/application
git commit -m "feat(delivery): add neighborhood use cases and facade"
```

---

## Task 4: Delivery — JPA persistence

**Files:**
- `infrastructure/persistence/jpa/NeighborhoodJpaEntity.java`
- `infrastructure/persistence/repository/SpringNeighborhoodJpaRepository.java`
- `infrastructure/persistence/mapper/NeighborhoodMapper.java`
- `infrastructure/persistence/adapter/NeighborhoodRepositoryAdapter.java`

Patterns identical to Phase 1.C Task 9 (Category). Notes:
- Table `neighborhoods` (defined in Task 8 migration below).
- Columns: `id UUID`, `name VARCHAR(120)`, `city VARCHAR(120)`, `fee NUMERIC(12,2)`, `currency VARCHAR(3) DEFAULT 'BRL'`, `active BOOLEAN`, `created_at`, `updated_at`.
- `SpringNeighborhoodJpaRepository.findAllByActiveTrueOrderByCityAscNameAsc()` and `existsByNameAndCity`.
- Mapper rebuilds `Money` via `Money.of(BigDecimal, currency)`.

- [ ] **Step 4.1–4.4:** Entity, repo, mapper, adapter.
- [ ] **Step 4.5:** Commit `feat(delivery): add JPA persistence for Neighborhood`.

---

## Task 5: Delivery — REST API

**Files:**
- `api/rest/NeighborhoodAdminController.java` — `POST /api/v1/admin/neighborhoods`, `PUT /api/v1/admin/neighborhoods/{id}`, `DELETE`, `GET` (list all incl. inactive). Authority: `OWNER` or `MANAGER`.
- `api/rest/PublicDeliveryController.java` — `GET /api/v1/delivery/neighborhoods` (active only). `GET /api/v1/delivery/fee?neighborhoodId=...` returns `{ "fee": 8.50, "currency": "BRL" }` or 404.
- `api/dto/NeighborhoodRequest.java` and `NeighborhoodResponse.java` (records).

Mirror catalog admin controller shape exactly: `@PreAuthorize("hasAnyRole('OWNER','MANAGER')")` (admin), `permitAll` for public paths (configured in Task 14 SecurityConfig change).

- [ ] **Step 5.1:** DTOs.
- [ ] **Step 5.2:** Admin controller (4 endpoints).
- [ ] **Step 5.3:** Public controller (2 endpoints).
- [ ] **Step 5.4:** Commit `feat(delivery): add admin and public REST controllers`.

---

## Task 6: Ordering module skeleton

- [ ] **Step 6.1:** `src/main/java/com/cardapio/ordering/package-info.java`
```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Ordering",
    allowedDependencies = {
        "shared",
        "api::error",
        "api::support",
        "catalog::application::CatalogFacade",
        "delivery::DeliveryFacade",
        "identity::api"   // only to read authenticated principal
    }
)
package com.cardapio.ordering;
```

If `catalog::application` is not yet a `@NamedInterface`, add `@org.springframework.modulith.NamedInterface("CatalogFacade")` to the `CatalogFacade.java` file in catalog. Same for `DeliveryFacade` (done in Task 3). Verify via `ModulithVerificationTest` after each addition.

- [ ] **Step 6.2:** Run modulith verifier — should still pass.
- [ ] **Step 6.3:** Commit `chore(ordering): add module skeleton`.

---

## Task 7: Ordering — value objects + IDs

**Files** (under `ordering/domain/model/`):
- 4 ID records (`CartId`, `CartItemId`, `OrderId`, `OrderItemId`) — same pattern as `CategoryId`.
- `OrderModality` enum: `DELIVERY`, `PICKUP`. (`DINE_IN` reserved name is in Phase 5.)
- `OrderStatus` enum **with transition rules baked in** (this is the only enum that's non-trivial; show full code below).
- `DeliveryAddress` VO — `street`, `number`, `complement?`, `district`, `city`, `PostalCode`, `NeighborhoodId neighborhoodId`. Validate non-blanks.
- `SelectedVariation(UUID variationId, String name, Money priceModifier)` — snapshot record; validate non-null; modifier may be zero.
- `SelectedAddOn(UUID groupId, UUID itemId, String name, Money price, int quantity)` — snapshot; quantity ≥ 1.
- `HalfAndHalf(UUID leftProductId, UUID rightProductId, String displayName, Money basePrice)` — snapshot; left/right non-null & distinct.
- `Observation` — wraps `String value` ≤ 200 chars trimmed; static `Observation.empty()`.

- [ ] **Step 7.1:** ID records (4 files).

- [ ] **Step 7.2:** `OrderStatus` with transition table

```java
package com.cardapio.ordering.domain.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    RECEIVED, CONFIRMED, PREPARING, READY, OUT_FOR_DELIVERY, PICKED_UP, DELIVERED, CANCELED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_DELIVERY = Map.of(
        RECEIVED,         EnumSet.of(CONFIRMED, CANCELED),
        CONFIRMED,        EnumSet.of(PREPARING, CANCELED),
        PREPARING,        EnumSet.of(READY),
        READY,            EnumSet.of(OUT_FOR_DELIVERY),
        OUT_FOR_DELIVERY, EnumSet.of(DELIVERED),
        DELIVERED,        EnumSet.noneOf(OrderStatus.class),
        CANCELED,         EnumSet.noneOf(OrderStatus.class)
    );

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_PICKUP = Map.of(
        RECEIVED,  EnumSet.of(CONFIRMED, CANCELED),
        CONFIRMED, EnumSet.of(PREPARING, CANCELED),
        PREPARING, EnumSet.of(READY),
        READY,     EnumSet.of(PICKED_UP),
        PICKED_UP, EnumSet.noneOf(OrderStatus.class),
        CANCELED,  EnumSet.noneOf(OrderStatus.class)
    );

    public boolean canTransitionTo(OrderStatus target, OrderModality modality) {
        var table = switch (modality) {
            case DELIVERY -> ALLOWED_DELIVERY;
            case PICKUP   -> ALLOWED_PICKUP;
        };
        return table.getOrDefault(this, EnumSet.noneOf(OrderStatus.class)).contains(target);
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == PICKED_UP || this == CANCELED;
    }
}
```

- [ ] **Step 7.3:** Other VOs (5 files). Use records where the type has no behaviour beyond accessors.

- [ ] **Step 7.4:** Tests
- `OrderStatusTest` — table-driven: every (status, modality, target) tuple, asserting `canTransitionTo` matches expected matrix; `isTerminal` true only for DELIVERED, PICKED_UP, CANCELED.
- `DeliveryAddressTest` — non-blank validation.
- `SelectedAddOnTest` — quantity ≥ 1.
- `HalfAndHalfTest` — left ≠ right.
- `ObservationTest` — trims, rejects > 200 chars, `empty()` works.

- [ ] **Step 7.5:** Commit `feat(ordering): add value objects and OrderStatus state machine`.

---

## Task 8: Ordering — Cart aggregate

**Files:**
- `domain/model/Cart.java` + `CartItem.java`
- Test: `CartTest.java`, `CartItemTest.java`

Cart is a **mutable** aggregate. Customer has at most one active cart. Cart has `customerId`, `OrderModality? plannedModality` (nullable until checkout), `DeliveryAddress? plannedAddress`, `List<CartItem> items`, `Instant createdAt`, `Instant updatedAt`.

CartItem has: `id`, `productId`, `Optional<SelectedVariation>`, `List<SelectedAddOn>`, `Optional<HalfAndHalf>`, `Observation observation`, `int quantity` (≥ 1).

**Important:** Cart **does not store prices**. Pricing happens at checkout against `catalog`. The cart only stores **which** product/variation/addon was chosen.

- [ ] **Step 8.1:** `CartItem` aggregate-internal entity. Methods: `changeQuantity(int)`, `replaceObservation(Observation)`. Constructor validates qty ≥ 1, observation non-null, productId non-null.

- [ ] **Step 8.2:** `Cart` aggregate root.
```java
public final class Cart {
    private final CartId id;
    private final UUID customerId;
    private final List<CartItem> items;
    private final Instant createdAt;
    private Instant updatedAt;

    // factories: createEmpty(customerId), rehydrate(...)
    public static Cart createEmpty(UUID customerId) { ... }

    public CartItem addItem(UUID productId, Optional<SelectedVariation> variation,
                            List<SelectedAddOn> addOns, Optional<HalfAndHalf> half,
                            Observation observation, int quantity, Clock clock) {
        var item = new CartItem(CartItemId.newId(), productId, variation, addOns, half, observation, quantity);
        items.add(item);
        updatedAt = clock.instant();
        return item;
    }

    public void updateItem(CartItemId itemId, int quantity, Observation observation, Clock clock) {
        var item = items.stream().filter(i -> i.id().equals(itemId)).findFirst()
            .orElseThrow(() -> new CartItemNotFoundException(itemId));
        item.changeQuantity(quantity);
        item.replaceObservation(observation);
        updatedAt = clock.instant();
    }

    public void removeItem(CartItemId itemId, Clock clock) {
        if (!items.removeIf(i -> i.id().equals(itemId)))
            throw new CartItemNotFoundException(itemId);
        updatedAt = clock.instant();
    }

    public void clear(Clock clock) { items.clear(); updatedAt = clock.instant(); }
    public boolean isEmpty() { return items.isEmpty(); }
    public List<CartItem> items() { return List.copyOf(items); }
    // accessors...
}
```

- [ ] **Step 8.3:** Tests cover: add → contains 1, update → quantity changed, remove → empty, removing missing item throws, updating missing item throws, clear empties.

- [ ] **Step 8.4:** Commit `feat(ordering): add Cart aggregate`.

---

## Task 9: Ordering — Order aggregate

**Files:**
- `domain/model/Order.java` + `OrderItem.java`
- `domain/exception/IllegalStatusTransitionException.java`
- Tests: `OrderTest.java`

OrderItem is the **frozen snapshot** of a CartItem at placement time. Fields: `id`, `productId`, `productName` (snapshot), `Optional<SelectedVariation>`, `List<SelectedAddOn>`, `Optional<HalfAndHalf>`, `Observation`, `int quantity`, `Money lineTotal` (computed and frozen).

Order fields: `id`, `customerId`, `OrderModality modality`, `OrderStatus status`, `List<OrderItem> items`, `Money subtotal`, `Money deliveryFee`, `Money discount`, `Money total`, `Optional<DeliveryAddress> address`, `Instant placedAt`, `Instant updatedAt`. Status starts at `RECEIVED`.

- [ ] **Step 9.1:** `OrderItem` record (or class) with computed `lineTotal`. Static factory `OrderItem.from(snapshot fields…, Money baseProductPrice)` computes `lineTotal = (baseProductPrice + variationModifier + sum(addonPrice * addonQty)) * itemQuantity`. Half-and-half uses the **higher** base of the two halves (business rule per spec implicit; note in code comment `// per spec: meio-a-meio cobra o de maior valor`).

- [ ] **Step 9.2:** `Order.place(...)`:
```java
public static Order place(UUID customerId, OrderModality modality,
                          List<OrderItem> items, Money deliveryFee,
                          Optional<DeliveryAddress> address, Clock clock) {
    if (items.isEmpty()) throw new IllegalArgumentException("order has no items");
    if (modality == OrderModality.DELIVERY && address.isEmpty())
        throw new IllegalArgumentException("delivery requires address");
    if (modality == OrderModality.PICKUP && deliveryFee.isPositive())
        throw new IllegalArgumentException("pickup must have zero fee");
    var subtotal = items.stream().map(OrderItem::lineTotal)
        .reduce(Money.zero(Currency.getInstance("BRL")), Money::add);
    var total = subtotal.add(deliveryFee);   // discount=0 in this phase
    return new Order(OrderId.newId(), customerId, modality, OrderStatus.RECEIVED,
                     new ArrayList<>(items), subtotal, deliveryFee,
                     Money.zero(subtotal.currency()), total, address,
                     clock.instant(), clock.instant());
}
```

- [ ] **Step 9.3:** `Order.advance(OrderStatus next, Clock clock)`:
```java
public void advance(OrderStatus next, Clock clock) {
    if (!status.canTransitionTo(next, modality))
        throw new IllegalStatusTransitionException(status, next, modality);
    this.status = next;
    this.updatedAt = clock.instant();
}

public void cancel(Clock clock) {
    if (status.isTerminal())
        throw new IllegalStatusTransitionException(status, OrderStatus.CANCELED, modality);
    if (status == OrderStatus.PREPARING || status == OrderStatus.READY ||
        status == OrderStatus.OUT_FOR_DELIVERY || status == OrderStatus.PICKED_UP)
        throw new IllegalStatusTransitionException(status, OrderStatus.CANCELED, modality);
    this.status = OrderStatus.CANCELED;
    this.updatedAt = clock.instant();
}
```

- [ ] **Step 9.4:** `IllegalStatusTransitionException extends DomainException` with code `ORDER_INVALID_TRANSITION`.

- [ ] **Step 9.5:** Tests
- `place computes subtotal from items, adds delivery fee for DELIVERY`.
- `place rejects empty items / DELIVERY without address / PICKUP with positive fee`.
- `advance from RECEIVED to CONFIRMED ok; to PREPARING illegal`.
- Full happy path matrix per modality.
- `cancel after PREPARING throws`.

- [ ] **Step 9.6:** Commit `feat(ordering): add Order aggregate with status workflow`.

---

## Task 10: Ordering — domain events + ports

**Files:**
- `domain/event/OrderPlaced.java`, `OrderStatusChanged.java`, `OrderCanceled.java` (records implementing `DomainEvent`)
- `domain/port/CartRepository.java`, `OrderRepository.java`, `CatalogQueryPort.java`, `DeliveryFeeQueryPort.java`, `IdempotencyKeyStore.java`

- [ ] **Step 10.1:** Events
```java
public record OrderPlaced(OrderId orderId, UUID customerId, OrderModality modality,
                          Money total, Instant placedAt) implements DomainEvent {}

public record OrderStatusChanged(OrderId orderId, OrderStatus from, OrderStatus to,
                                 Instant changedAt) implements DomainEvent {}

public record OrderCanceled(OrderId orderId, UUID customerId, Instant canceledAt) implements DomainEvent {}
```

- [ ] **Step 10.2:** Repository ports
```java
public interface CartRepository {
    Optional<Cart> findByCustomerId(UUID customerId);
    void save(Cart cart);
    void delete(Cart cart);
}

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId id);
    List<Order> findByCustomerId(UUID customerId, int limit, int offset);
    Page<Order> findAllAdmin(OrderStatus filterOrNull, Instant fromOrNull, Instant toOrNull, Pageable pageable);
}
```

- [ ] **Step 10.3:** Cross-context query ports (these break the dependency on catalog/delivery JPA):
```java
public interface CatalogQueryPort {
    Optional<ProductSnapshot> loadProduct(UUID productId);

    record ProductSnapshot(
        UUID productId,
        String name,
        Money basePrice,
        boolean available,
        boolean stockTracked,
        int stockQuantity,
        boolean allowsHalfHalf,
        List<VariationSnapshot> variations,
        List<AddOnGroupSnapshot> addOnGroups
    ) {}
    record VariationSnapshot(UUID id, String name, Money priceModifier) {}
    record AddOnGroupSnapshot(UUID id, String name, int minSelection, int maxSelection,
                              List<AddOnItemSnapshot> items) {}
    record AddOnItemSnapshot(UUID id, String name, Money price) {}
}

public interface DeliveryFeeQueryPort {
    Optional<Money> getActiveFee(UUID neighborhoodId);
}
```

- [ ] **Step 10.4:** `IdempotencyKeyStore`
```java
public interface IdempotencyKeyStore {
    /**
     * Reserves the key. Returns Optional.empty() if it's the first time;
     * returns Optional.of(orderId) if the key was already used.
     * The implementation is unique-constraint based: collisions are returned, not thrown.
     */
    Optional<OrderId> findExisting(String key, UUID customerId);
    void register(String key, UUID customerId, OrderId orderId);
}
```

- [ ] **Step 10.5:** Commit `feat(ordering): add domain events and ports`.

---

## Task 11: Flyway V6 migration — delivery + ordering tables

**File:** `src/main/resources/db/migration/V6__delivery_and_ordering_tables.sql`

```sql
-- ============================================================================
-- delivery
-- ============================================================================
CREATE TABLE neighborhoods (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    city VARCHAR(120) NOT NULL,
    fee NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_neighborhood_name_city UNIQUE (name, city)
);
CREATE INDEX idx_neighborhoods_active ON neighborhoods (active) WHERE active = TRUE;

-- ============================================================================
-- ordering — carts
-- ============================================================================
CREATE TABLE carts (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL UNIQUE,           -- one active cart per customer
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    variation_id UUID,
    half_left_product_id UUID,
    half_right_product_id UUID,
    observation VARCHAR(200) NOT NULL DEFAULT '',
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    position INTEGER NOT NULL DEFAULT 0,
    selections_json JSONB NOT NULL DEFAULT '[]'  -- array of {groupId, itemId, quantity}
);
CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);

-- ============================================================================
-- ordering — orders (frozen snapshot)
-- ============================================================================
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    modality VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    delivery_fee NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    address_street VARCHAR(160),
    address_number VARCHAR(20),
    address_complement VARCHAR(120),
    address_district VARCHAR(120),
    address_city VARCHAR(120),
    address_postal_code VARCHAR(16),
    address_neighborhood_id UUID,
    placed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_orders_customer ON orders (customer_id, placed_at DESC);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_placed_at ON orders (placed_at DESC);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(180) NOT NULL,
    variation_id UUID,
    variation_name VARCHAR(80),
    variation_modifier NUMERIC(12, 2),
    half_left_product_id UUID,
    half_right_product_id UUID,
    half_display_name VARCHAR(180),
    half_base_price NUMERIC(12, 2),
    observation VARCHAR(200) NOT NULL DEFAULT '',
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    line_total NUMERIC(12, 2) NOT NULL,
    selections_json JSONB NOT NULL DEFAULT '[]'  -- snapshot: [{groupId,itemId,name,price,qty}]
);
CREATE INDEX idx_order_items_order ON order_items (order_id);

-- ============================================================================
-- ordering — idempotency
-- ============================================================================
CREATE TABLE idempotency_keys (
    customer_id UUID NOT NULL,
    key VARCHAR(120) NOT NULL,
    order_id UUID NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (customer_id, key)
);
CREATE INDEX idx_idempotency_keys_order ON idempotency_keys (order_id);
```

- [ ] **Step 11.1:** Write the file.
- [ ] **Step 11.2:** Run `./mvnw test -Dtest=SmokeIntegrationTest` — Flyway should apply V6 cleanly.
- [ ] **Step 11.3:** Commit `feat(ordering,delivery): add V6 migration`.

---

## Task 12: Ordering — JPA persistence (Cart, Order, Idempotency)

Mirror Phase 1.C Task 10 patterns (entity per table, Spring Data repo, mapper, adapter that owns `Clock`).

**Files in `infrastructure/persistence/`:**

- [ ] **Step 12.1:** `jpa/CartJpaEntity.java` (`@OneToMany(cascade=ALL, orphanRemoval=true)` to `CartItemJpaEntity` with `JoinColumn("cart_id")`).
- [ ] **Step 12.2:** `jpa/CartItemJpaEntity.java` — store `selections_json` via `@JdbcTypeCode(SqlTypes.JSON)` (Hibernate 6 native JSON support; already on classpath via `hibernate-core` 6.x). Map JSON to a `List<AddOnSelectionDto>` plain record.
- [ ] **Step 12.3:** `jpa/OrderJpaEntity.java` + `jpa/OrderItemJpaEntity.java` — same `@OneToMany` cascade. Address fields embedded as flat columns (no `@Embeddable` to keep it simple).
- [ ] **Step 12.4:** `jpa/IdempotencyKeyJpaEntity.java` with composite key `@IdClass(IdempotencyKeyId.class)` (or `@Embeddable` `IdempotencyKeyId` with `@EmbeddedId`).
- [ ] **Step 12.5:** `repository/SpringCartJpaRepository` — `findByCustomerId(UUID)`.
- [ ] **Step 12.6:** `repository/SpringOrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID>, JpaSpecificationExecutor<OrderJpaEntity>` — admin filter uses Specifications. `findAllByCustomerIdOrderByPlacedAtDesc(UUID, Pageable)` for `/me/orders`.
- [ ] **Step 12.7:** `repository/SpringIdempotencyJpaRepository` — `findByCustomerIdAndKey`.
- [ ] **Step 12.8:** `mapper/CartMapper.java` — bidirectional. JSON column ↔ `List<SelectedAddOn>`.
- [ ] **Step 12.9:** `mapper/OrderMapper.java` — bidirectional, including all snapshot fields.
- [ ] **Step 12.10:** `adapter/CartRepositoryAdapter.java` (implements `CartRepository`).
- [ ] **Step 12.11:** `adapter/OrderRepositoryAdapter.java` (implements `OrderRepository`, builds Specifications for admin filter).
- [ ] **Step 12.12:** `adapter/JpaIdempotencyKeyStore.java`:
```java
@Component
@Transactional
public class JpaIdempotencyKeyStore implements IdempotencyKeyStore {
    private final SpringIdempotencyJpaRepository repo;

    @Override
    public Optional<OrderId> findExisting(String key, UUID customerId) {
        return repo.findByCustomerIdAndKey(customerId, key)
                   .map(e -> OrderId.of(e.getOrderId()));
    }

    @Override
    public void register(String key, UUID customerId, OrderId orderId) {
        repo.save(new IdempotencyKeyJpaEntity(customerId, key, orderId.value(), Instant.now()));
    }
}
```

- [ ] **Step 12.13:** `./mvnw test -Dtest=SmokeIntegrationTest` — Hibernate validates schema.
- [ ] **Step 12.14:** Commit `feat(ordering): add JPA persistence`.

---

## Task 13: Ordering — cross-context adapters

**Files (in `infrastructure/catalog/`):**
- `CatalogQueryAdapter.java` (implements `CatalogQueryPort` by calling `CatalogFacade.getProductDetails` + mapping `ProductDetailsView` → `ProductSnapshot`).
- `DeliveryFeeQueryAdapter.java` (implements `DeliveryFeeQueryPort` by calling `DeliveryFacade.getById`).

```java
@Component
class CatalogQueryAdapter implements CatalogQueryPort {
    private final CatalogFacade catalog;
    public CatalogQueryAdapter(CatalogFacade catalog) { this.catalog = catalog; }

    @Override
    public Optional<ProductSnapshot> loadProduct(UUID productId) {
        try {
            var v = catalog.getProductDetails(ProductId.of(productId));
            return Optional.of(toSnapshot(v));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }
    // toSnapshot: walk variations + addOnGroups + addOnItems and rebuild snapshot records
}
```

- [ ] **Step 13.1:** Both adapters with mappers.
- [ ] **Step 13.2:** Run modulith verifier — confirm allowed dependencies hold.
- [ ] **Step 13.3:** Commit `feat(ordering): wire catalog and delivery query adapters`.

---

## Task 14: Ordering — Cart use cases (Add/Update/Remove/Get)

**Files in `application/`:**

- [ ] **Step 14.1:** Commands
```java
public record AddCartItemCommand(UUID customerId, UUID productId, UUID variationId,
                                 List<AddOnSelection> addOns, UUID halfLeftProductId,
                                 UUID halfRightProductId, String observation, int quantity) {
    public record AddOnSelection(UUID groupId, UUID itemId, int quantity) {}
}
public record UpdateCartItemCommand(UUID customerId, UUID cartItemId, int quantity, String observation) {}
public record RemoveCartItemCommand(UUID customerId, UUID cartItemId) {}
```

- [ ] **Step 14.2:** `AddCartItemUseCase`:
1. `cartRepo.findByCustomerId` → if absent, create empty.
2. Validate against catalog via `CatalogQueryPort.loadProduct(productId)`:
   - Product exists and `available == true`.
   - If `variationId != null` → exists in product's variations.
   - For each addOn → group exists in product, item exists in group, qty within group's `[minSelection, maxSelection]` aggregate constraints. (Min/max are validated *aggregately per group*: sum of selected items in that group must be in range.)
   - If `halfLeft/halfRightProductId` provided → product `allowsHalfHalf == true` AND both halves exist as products. Half-and-half is mutually exclusive with `variationId` — if both, fail.
   - Stock: if `stockTracked && stockQuantity < quantity` → `Notification` error `PRODUCT_OUT_OF_STOCK`.
3. Build a `SelectedVariation` / `List<SelectedAddOn>` / `Optional<HalfAndHalf>` snapshot from the catalog data and add the item to cart.
4. Save cart. Return `CartItemId`.

Return `Result<CartItemId>` — failures use `Notification.addError`. Throw `NotificationException` from facade.

- [ ] **Step 14.3:** `UpdateCartItemUseCase` — only `quantity` and `observation` are editable. Validate qty ≥ 1.

- [ ] **Step 14.4:** `RemoveCartItemUseCase` — delegates to `Cart.removeItem`.

- [ ] **Step 14.5:** `GetCartQuery` — returns `CartView` populated with **fresh prices from catalog** (so the customer sees current totals before checkout). Each line price is computed by `CartPricingService` (Task 15).

- [ ] **Step 14.6:** `CartView`:
```java
public record CartView(UUID id, UUID customerId, List<CartItemView> items,
                       BigDecimal subtotal, String currency, boolean hasUnavailableItems) {}
public record CartItemView(UUID id, UUID productId, String productName, String variationName,
                           List<String> addOnNames, String halfDescription, String observation,
                           int quantity, BigDecimal lineTotal, boolean available) {}
```

- [ ] **Step 14.7:** Tests with fake `CartRepository` and fake `CatalogQueryPort` — validation matrix.

- [ ] **Step 14.8:** Commit `feat(ordering): add cart use cases`.

---

## Task 15: Ordering — CartPricingService + PlaceOrderUseCase

**File:** `application/usecase/CartPricingService.java`

Pure domain logic that takes a `Cart` plus a function `UUID → ProductSnapshot` and returns either:
- a `Notification` with all unavailable / out-of-stock items, or
- a `List<OrderItem>` ready to place.

```java
@Service
class CartPricingService {
    private final CatalogQueryPort catalog;
    public CartPricingService(CatalogQueryPort catalog) { this.catalog = catalog; }

    /** Returns Result.success(items) or Result.failure(notification). */
    public Result<List<OrderItem>> price(Cart cart) {
        var notification = Notification.empty();
        var items = new ArrayList<OrderItem>();

        for (var ci : cart.items()) {
            var snapshot = catalog.loadProduct(ci.productId()).orElse(null);
            if (snapshot == null) { notification.addError("cart.item." + ci.id() + ".product",
                "PRODUCT_NOT_FOUND", "Produto não encontrado"); continue; }
            if (!snapshot.available()) { notification.addError("cart.item." + ci.id(),
                "PRODUCT_UNAVAILABLE", "Produto " + snapshot.name() + " indisponível"); continue; }
            if (snapshot.stockTracked() && snapshot.stockQuantity() < ci.quantity()) {
                notification.addError("cart.item." + ci.id(),
                    "PRODUCT_OUT_OF_STOCK", "Estoque insuficiente para " + snapshot.name()); continue;
            }
            // re-resolve variation/addons against current snapshot to get fresh prices
            // build OrderItem with frozen pricing
            items.add(OrderItem.from(snapshot, ci));
        }
        if (notification.hasErrors()) return Result.failure(notification);
        return Result.success(List.copyOf(items));
    }
}
```

- [ ] **Step 15.1:** `CartPricingService`.

- [ ] **Step 15.2:** `PlaceOrderCommand`
```java
public record PlaceOrderCommand(UUID customerId, OrderModality modality,
                                DeliveryAddressDto address, String idempotencyKey) {
    public record DeliveryAddressDto(String street, String number, String complement,
                                     String district, String city, String postalCode,
                                     UUID neighborhoodId) {}
}
```

- [ ] **Step 15.3:** `PlaceOrderUseCase`
```java
@Service
public class PlaceOrderUseCase {
    private final CartRepository carts;
    private final OrderRepository orders;
    private final CartPricingService pricing;
    private final DeliveryFeeQueryPort deliveryFees;
    private final IdempotencyKeyStore idempotency;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public Result<PlacedOrderView> execute(PlaceOrderCommand cmd) {
        // 1. Idempotency short-circuit
        var existing = idempotency.findExisting(cmd.idempotencyKey(), cmd.customerId());
        if (existing.isPresent()) {
            var order = orders.findById(existing.get())
                .orElseThrow(() -> new IllegalStateException("idempotency points to missing order"));
            return Result.success(PlacedOrderView.from(order));
        }

        // 2. Load cart
        var cart = carts.findByCustomerId(cmd.customerId()).orElse(null);
        var n = Notification.empty();
        if (cart == null || cart.isEmpty()) {
            n.addError("cart", "CART_EMPTY", "Carrinho vazio");
            return Result.failure(n);
        }

        // 3. Price + validate items against catalog
        var priced = pricing.price(cart);
        if (priced instanceof Result.Failure<List<OrderItem>> f) return Result.failure(f.notification());
        var items = ((Result.Success<List<OrderItem>>) priced).value();

        // 4. Compute delivery fee
        Money fee = Money.zero(items.get(0).lineTotal().currency());
        Optional<DeliveryAddress> address = Optional.empty();
        if (cmd.modality() == OrderModality.DELIVERY) {
            if (cmd.address() == null) {
                n.addError("address", "ADDRESS_REQUIRED", "Endereço é obrigatório para entrega");
                return Result.failure(n);
            }
            var feeOpt = deliveryFees.getActiveFee(cmd.address().neighborhoodId());
            if (feeOpt.isEmpty()) {
                n.addError("address.neighborhood", "NEIGHBORHOOD_NOT_SERVED",
                    "Bairro não atendido"); return Result.failure(n);
            }
            fee = feeOpt.get();
            address = Optional.of(toDeliveryAddress(cmd.address()));
        }

        // 5. Build Order
        var order = Order.place(cmd.customerId(), cmd.modality(), items, fee, address, clock);

        // 6. Persist order + idempotency key (same TX)
        orders.save(order);
        idempotency.register(cmd.idempotencyKey(), cmd.customerId(), order.id());

        // 7. Empty cart
        cart.clear(clock);
        carts.save(cart);

        // 8. Publish event (Modulith outbox — same TX)
        events.publishEvent(new OrderPlaced(order.id(), order.customerId(),
            order.modality(), order.total(), order.placedAt()));

        return Result.success(PlacedOrderView.from(order));
    }
}
```

- [ ] **Step 15.4:** `PlacedOrderView` and helper `toDeliveryAddress`.

- [ ] **Step 15.5:** Tests
- Idempotency: same key → same order returned, no duplicate.
- Empty cart → `CART_EMPTY`.
- Unavailable product → `PRODUCT_UNAVAILABLE`.
- DELIVERY without `neighborhoodId served` → `NEIGHBORHOOD_NOT_SERVED`.
- PICKUP — no address required, fee = 0.
- Happy path: order saved with status `RECEIVED`, cart cleared, event captured by `ApplicationEvents` test util.

- [ ] **Step 15.6:** Commit `feat(ordering): add place order use case with idempotency`.

---

## Task 16: Ordering — admin status transition + cancel

**Files:**
- `command/AdvanceOrderStatusCommand`, `CancelOrderCommand`
- `usecase/AdvanceOrderStatusUseCase`, `CancelOrderUseCase`

- [ ] **Step 16.1:** `AdvanceOrderStatusUseCase`:
```java
@Transactional
public Result<Void> execute(AdvanceOrderStatusCommand cmd) {
    var order = orders.findById(cmd.orderId())
        .orElseThrow(() -> new OrderNotFoundException(cmd.orderId()));
    var prev = order.status();
    try {
        order.advance(cmd.target(), clock);
    } catch (IllegalStatusTransitionException e) {
        var n = Notification.empty();
        n.addError("status", "ORDER_INVALID_TRANSITION",
            "Transição inválida: " + prev + " → " + cmd.target());
        return Result.failure(n);
    }
    orders.save(order);
    events.publishEvent(new OrderStatusChanged(order.id(), prev, order.status(), order.updatedAt()));
    return Result.success(null);
}
```

- [ ] **Step 16.2:** `CancelOrderUseCase` — only OWNER/MANAGER can cancel; emits `OrderCanceled`.

- [ ] **Step 16.3:** Tests cover full happy paths per modality + invalid transitions.

- [ ] **Step 16.4:** Commit `feat(ordering): add admin status transition use cases`.

---

## Task 17: Ordering — query use cases

- [ ] **Step 17.1:** `GetOrderQuery(OrderId, UUID requesterCustomerIdOrNull)` — if requester provided, must own order; else admin path.
- [ ] **Step 17.2:** `ListMyOrdersQuery(UUID customerId, int limit, int offset)` → `List<OrderSummaryView>`.
- [ ] **Step 17.3:** `ListOrdersAdminQuery` — supports filter by status / date range / pagination via Specification.
- [ ] **Step 17.4:** Views: `OrderView`, `OrderItemView`, `OrderSummaryView`.
- [ ] **Step 17.5:** Commit `feat(ordering): add order query use cases`.

---

## Task 18: Ordering — facade

```java
public interface OrderingFacade {
    // Cart
    UUID addCartItem(AddCartItemCommand cmd) throws NotificationException;
    void updateCartItem(UpdateCartItemCommand cmd) throws NotificationException;
    void removeCartItem(RemoveCartItemCommand cmd) throws NotificationException;
    CartView getMyCart(UUID customerId);

    // Orders — customer
    PlacedOrderView placeOrder(PlaceOrderCommand cmd) throws NotificationException;
    OrderView getMyOrder(UUID customerId, OrderId orderId) throws NotFoundException;
    List<OrderSummaryView> listMyOrders(UUID customerId, int limit, int offset);

    // Orders — admin
    OrderView getOrderAdmin(OrderId orderId) throws NotFoundException;
    Page<OrderSummaryView> listOrdersAdmin(OrderStatus status, Instant from, Instant to, Pageable pageable);
    void advanceStatus(AdvanceOrderStatusCommand cmd) throws NotificationException;
    void cancelOrder(CancelOrderCommand cmd) throws NotificationException;
}
```

`OrderingFacadeImpl` — `@Service` delegating to use cases. Throws `NotificationException` when `Result.Failure`.

- [ ] **Step 18.1:** Facade interface + impl.
- [ ] **Step 18.2:** Commit `feat(ordering): add facade`.

---

## Task 19: SecurityConfig wiring

Modify [src/main/java/com/cardapio/identity/api/security/SecurityConfig.java](../../../../src/main/java/com/cardapio/identity/api/security/SecurityConfig.java).

- [ ] **Step 19.1:** Add path matchers in this order (more specific first):

```java
.requestMatchers(HttpMethod.GET,
    "/api/v1/menu/**", "/api/v1/operating-hours",
    "/api/v1/delivery/neighborhoods", "/api/v1/delivery/fee").permitAll()

// Customer-authenticated cart + orders
.requestMatchers("/api/v1/cart/**", "/api/v1/orders/**", "/api/v1/me/orders/**")
    .hasRole("CUSTOMER")

// Admin (existing scheme — extend)
.requestMatchers("/api/v1/admin/neighborhoods/**", "/api/v1/admin/orders/**")
    .hasAnyRole("OWNER", "MANAGER", "OPERATOR")
```

`OPERATOR` may advance status but not cancel — enforce per-method via `@PreAuthorize("hasAnyRole('OWNER','MANAGER')")` on `cancelOrder`.

- [ ] **Step 19.2:** Modulith verifier still passes.
- [ ] **Step 19.3:** Commit `feat(security): allow ordering and delivery paths`.

---

## Task 20: REST API — Cart + Order (customer)

**Files:**
- `api/dto/AddCartItemRequest`, `UpdateCartItemRequest`, `CartResponse`, `CartItemResponse`, `PlaceOrderRequest`, `PlaceOrderResponse`, `OrderResponse`, `OrderSummaryResponse`.
- `api/rest/CartController.java`, `api/rest/OrderController.java`.

- [ ] **Step 20.1:** Request DTOs with `@NotNull` / `@Min(1)` / `@Size(max=200)` jakarta-validation.

- [ ] **Step 20.2:** `CartController`:
```java
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    private final OrderingFacade ordering;

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal CustomerPrincipal me) {
        return CartResponse.from(ordering.getMyCart(me.id()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> add(@AuthenticationPrincipal CustomerPrincipal me,
                                                @RequestBody @Valid AddCartItemRequest body) {
        var id = ordering.addCartItem(body.toCommand(me.id()));
        return ResponseEntity.created(URI.create("/api/v1/cart/items/" + id))
                             .body(new CartItemResponse(id));
    }

    @PutMapping("/items/{itemId}")
    public void update(@AuthenticationPrincipal CustomerPrincipal me, @PathVariable UUID itemId,
                       @RequestBody @Valid UpdateCartItemRequest body) {
        ordering.updateCartItem(new UpdateCartItemCommand(me.id(), itemId, body.quantity(), body.observation()));
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal CustomerPrincipal me, @PathVariable UUID itemId) {
        ordering.removeCartItem(new RemoveCartItemCommand(me.id(), itemId));
    }
}
```

- [ ] **Step 20.3:** `OrderController`:
```java
@PostMapping("/api/v1/orders")
public ResponseEntity<PlaceOrderResponse> place(
        @AuthenticationPrincipal CustomerPrincipal me,
        @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
        @RequestBody @Valid PlaceOrderRequest body) {
    var view = ordering.placeOrder(body.toCommand(me.id(), idempotencyKey));
    return ResponseEntity.created(URI.create("/api/v1/orders/" + view.id()))
                         .body(PlaceOrderResponse.from(view));
}

@GetMapping("/api/v1/orders/{id}")
public OrderResponse get(@AuthenticationPrincipal CustomerPrincipal me, @PathVariable UUID id) {
    return OrderResponse.from(ordering.getMyOrder(me.id(), OrderId.of(id)));
}

@GetMapping("/api/v1/me/orders")
public List<OrderSummaryResponse> mine(@AuthenticationPrincipal CustomerPrincipal me,
                                       @RequestParam(defaultValue="20") int limit,
                                       @RequestParam(defaultValue="0") int offset) {
    return ordering.listMyOrders(me.id(), limit, offset)
                   .stream().map(OrderSummaryResponse::from).toList();
}
```

- [ ] **Step 20.4:** Confirm `Idempotency-Key` is required (controller advice or `@RequestHeader` non-required-false → 400 if missing).

- [ ] **Step 20.5:** Commit `feat(ordering): add customer cart and order REST controllers`.

---

## Task 21: REST API — admin orders

- [ ] **Step 21.1:** `OrderAdminController` — `GET /api/v1/admin/orders?status=&from=&to=&page=&size=`, `GET /api/v1/admin/orders/{id}`, `PATCH /api/v1/admin/orders/{id}/status` (body: `{ "status": "PREPARING" }`), `POST /api/v1/admin/orders/{id}/cancel` (cancel as a separate verb-style endpoint; admin-only OWNER/MANAGER).

- [ ] **Step 21.2:** Commit `feat(ordering): add admin orders REST controller`.

---

## Task 22: E2E test

**File:** `src/test/java/com/cardapio/ordering/api/OrderingE2ETest.java`

`@SpringBootTest` + `@AutoConfigureMockMvc` + `@Testcontainers` (matching catalog E2E shape).

Scenarios (Brazilian-Portuguese error messages OK):
1. **Setup:** create admin (OWNER), category, product available, neighborhood active.
2. Customer registers + logs in (uses `/api/v1/auth/register` + `/api/v1/auth/login`).
3. `POST /api/v1/cart/items` with valid product → 201.
4. `GET /api/v1/cart` → returns 1 item with computed `lineTotal`.
5. `POST /api/v1/orders` with `Idempotency-Key: abc-123`, modality DELIVERY, valid address → 201, body has `id` and `total = subtotal + fee`.
6. **Idempotency:** `POST /api/v1/orders` again with same `Idempotency-Key: abc-123` → 201 with **same** order id (no duplicate row in DB).
7. `GET /api/v1/orders/{id}` as the customer → 200, status `RECEIVED`. As another customer → 404 (not 403, to avoid leaking existence).
8. Admin `GET /api/v1/admin/orders` → list contains the new order.
9. Admin `PATCH /api/v1/admin/orders/{id}/status` to `PREPARING` from `RECEIVED` → 422 (must go through CONFIRMED first).
10. Admin status `CONFIRMED` → 200 → `PREPARING` → `READY` → `OUT_FOR_DELIVERY` → `DELIVERED` → final 200.
11. Cancel attempt on `DELIVERED` order → 422.

- [ ] **Step 22.1:** Implement test scenarios.
- [ ] **Step 22.2:** `./mvnw test -Dtest=OrderingE2ETest`.
- [ ] **Step 22.3:** Commit `test(ordering): add E2E happy + edge cases`.

---

## Task 23: Modulith verification + clean architecture check

- [ ] **Step 23.1:** `./mvnw test -Dtest=ModulithVerificationTest` — passes.
- [ ] **Step 23.2:** `./mvnw test -Dtest=CleanArchitectureTest` — domain has no Spring/JPA imports; api has no JPA imports.
- [ ] **Step 23.3:** Generate updated module documentation: `./mvnw test -Dtest=ModulithDocumentationTest`. Inspect new diagram in `target/spring-modulith-docs/`.

---

## Task 24: Final verification + tag

- [ ] **Step 24.1:** Full test run: `./mvnw test`. Expected: all green; no failures, no errors. New tests added in this phase: ~50–60.
- [ ] **Step 24.2:** Smoke run the app locally:
```bash
./mvnw spring-boot:run
```
And manually exercise (curl or Bruno collection):
- `POST /api/v1/admin/auth/login` → token.
- `POST /api/v1/admin/categories`, `POST /api/v1/admin/products`, `POST /api/v1/admin/neighborhoods` → seed data.
- `POST /api/v1/auth/register`, `POST /api/v1/auth/login` → customer token.
- `POST /api/v1/cart/items`, `GET /api/v1/cart`, `POST /api/v1/orders` (with `Idempotency-Key`).
- Admin advance status flow.

- [ ] **Step 24.3:** Tag:
```bash
git tag -a phase-2-complete -m "Phase 2: ordering + delivery (cart, order, status workflow, idempotency, events)"
```

---

## Acceptance Criteria

- ✅ `delivery` module: full CRUD admin, public list + fee endpoint.
- ✅ `ordering` module: cart with add/update/remove/get; checkout idempotent; order with frozen pricing snapshot; status workflow enforced for `DELIVERY` and `PICKUP`; admin can advance and cancel; customer can view own orders only.
- ✅ Domain events `OrderPlaced`, `OrderStatusChanged`, `OrderCanceled` published transactionally via Modulith outbox (already-configured tables `event_publication` / `event_publication_archive`). No subscribers in this phase — verified via `ApplicationEvents` test util.
- ✅ Cross-context boundary: `ordering` reads catalog/delivery only through facades and `@NamedInterface`. No FKs across schemas. Modulith verifier passes.
- ✅ All tests green; `CleanArchitectureTest` passes.

---

## Notes for next phases

- **Phase 3 (Payment):** subscribes `OrderPlaced` to create `PaymentTransaction`; publishes `PaymentApproved` → ordering listener advances `RECEIVED → CONFIRMED` automatically (replacing the manual admin step for this transition).
- **Phase 4 (Notification):** subscribes `OrderPlaced` and `OrderStatusChanged` to push e-mail / WhatsApp / SSE; also subscribes `ProductOutOfStock` (catalog publishes when stock decrement on `OrderPlaced` reaches zero — that listener also belongs to Phase 4).
- **Phase 5 (Mesa/Comanda):** adds `DINE_IN` to `OrderModality` and `OrderStatus.SERVED`. The `OrderStatus.canTransitionTo` table is intentionally split per modality so Phase 5 only adds an `ALLOWED_DINE_IN` map without touching delivery/pickup paths.
- **Phase 6 (Promotion):** adds `POST /cart/coupon` and `DELETE /cart/coupon`; `Order.discount` already exists in this phase but is always zero — the coupon application step plugs in before total computation in `PlaceOrderUseCase`.
