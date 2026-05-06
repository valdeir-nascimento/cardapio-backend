# Phase 6a — Promotions (Coupons) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the `promotion` bounded context. Admin manages coupon codes (`PERCENT` or `FIXED` discount, validity window, `minOrderValue`, `maxUses`, `usesCount`). Customers apply a coupon code to their cart and the discount lands on the placed `Order`. Coupon usage is incremented when an `OrderPlaced` event is observed and decremented on `OrderCanceled`, in line with the spec's event contract.

**Architecture:** Clean Architecture inside `com.cardapio.promotion`.
- Aggregate: `Coupon` (id, code, type, value, validFrom/validUntil, minOrderValue, maxUses, usesCount, active).
- Value type: `DiscountType { PERCENT, FIXED }`.
- Read-only port `CouponQueryPort` exposed via `@NamedInterface("CouponQueryPort")` so `ordering` can validate and price a coupon at cart/checkout time without depending on `promotion`'s JPA. The port returns a `CouponEvaluation` (subtotal-aware): either `Applicable(discountAmount)` or a typed failure (`NOT_FOUND`, `INACTIVE`, `EXPIRED`, `BELOW_MIN_ORDER`, `EXHAUSTED`). Pricing is shared so admin preview and customer apply give identical numbers.
- Two listeners inside `promotion`:
  - `@ApplicationModuleListener on(OrderPlaced)`: if the order references a coupon, increment `usesCount`. Idempotent on `(couponCode, orderId)` via `coupon_uses` table.
  - `@ApplicationModuleListener on(OrderCanceled)`: decrement once per `(couponCode, orderId)`.
- Use cases return `Result<T>` with Notification for business outcomes; exceptions for infra.

**Cross-context contracts:**
- `ordering::Cart` gains optional `couponCode` (set/clear use cases). Validation happens at apply-time and again at place-time (defense in depth — coupon may have expired between apply and checkout).
- `ordering::Order` already has a `discount` Money field. We start populating it. The applied coupon code is also persisted on the order so the listener can find it (`Order.appliedCouponCode`).
- `OrderPlaced` event gets a new optional `couponCode` field; `OrderCanceled` likewise. Both modules need updated event records (additive).
- `promotion` does not reach into `ordering` directly — the two listeners react to events.

**Tech Stack additions:** none. All in-process.

**Reference:**
- Spec: [docs/superpowers/specs/2026-05-04-cardapio-digital-backend-design.md](../specs/2026-05-04-cardapio-digital-backend-design.md) — §2 contexts, §3 promotion/ordering, §5 events table (`OrderPlaced` → `promotion: usesCount++`, `OrderCanceled` → `promotion: usesCount--`), §6.4 fluxo cupom, §9 Phase 6
- Phase 4 plan: [docs/superpowers/plans/2026-05-06-phase-4-notifications.md](./2026-05-06-phase-4-notifications.md) — `@ApplicationModuleListener` and Modulith outbox semantics
- Phase 5 plan: [docs/superpowers/plans/2026-05-06-phase-5-table-comanda.md](./2026-05-06-phase-5-table-comanda.md) — `@NamedInterface` and use-case shape inside `ordering`

**Out of scope (deferred):**
- Stacking multiple coupons (only one per cart in MVP).
- Per-product / per-category coupons — MVP applies discount on subtotal only.
- "First order" / "birthday" auto-coupons — admin-issued codes only.
- Free-delivery coupons — discount applies on subtotal only, never on `deliveryFee`.
- Customer-specific coupon allowlists — every coupon is globally redeemable up to `maxUses`.
- Coupon analytics dashboard — Phase 7.

---

## File Structure

```
src/main/java/com/cardapio/promotion/
├── domain/
│   ├── model/
│   │   ├── CouponId.java                          (record-VO)
│   │   ├── CouponCode.java                        (VO; uppercased, alnum + dash, max 32 chars)
│   │   ├── DiscountType.java                      (enum PERCENT, FIXED)
│   │   ├── Coupon.java                            (aggregate)
│   │   └── package-info.java                      (@NamedInterface("ids"))
│   ├── port/
│   │   ├── CouponRepository.java
│   │   ├── CouponQueryPort.java                   (cross-context port, @NamedInterface)
│   │   └── package-info.java                      (@NamedInterface("CouponQueryPort"))
│   ├── dto/
│   │   └── CouponEvaluation.java                  (sealed: Applicable / NotFound / Inactive /
│   │                                               Expired / BelowMinOrder / Exhausted)
│   └── exception/
│       └── CouponInvariantException.java
├── application/
│   ├── command/
│   │   ├── CreateCouponCommand.java
│   │   ├── UpdateCouponCommand.java
│   │   ├── DeactivateCouponCommand.java
│   │   └── EvaluateCouponCommand.java             (subtotal + currency for preview)
│   ├── usecase/
│   │   ├── CreateCouponUseCase.java
│   │   ├── UpdateCouponUseCase.java
│   │   ├── DeactivateCouponUseCase.java
│   │   ├── ListCouponsUseCase.java                (admin)
│   │   └── EvaluateCouponUseCase.java             (impl of CouponQueryPort)
│   ├── event/
│   │   ├── OrderPlacedCouponListener.java         (usesCount++)
│   │   └── OrderCanceledCouponListener.java       (usesCount-- once per orderId)
│   ├── dto/
│   │   ├── CouponView.java                        (admin read)
│   │   └── package-info.java                      (@NamedInterface("dto"))
│   └── PromotionFacade.java + Impl                (admin operations only; query exposed via port)
├── infrastructure/
│   └── persistence/
│       ├── jpa/
│       │   ├── CouponJpaEntity.java
│       │   └── CouponUseJpaEntity.java            (idempotency: PK = (coupon_code, order_id))
│       ├── repository/
│       │   ├── SpringCouponJpaRepository.java
│       │   └── SpringCouponUseJpaRepository.java
│       ├── mapper/
│       │   └── CouponMapper.java
│       └── adapter/
│           └── CouponRepositoryAdapter.java
├── api/
│   ├── rest/
│   │   └── AdminCouponController.java
│   └── dto/
│       ├── CreateCouponRequest.java
│       ├── UpdateCouponRequest.java
│       └── CouponResponse.java
└── package-info.java                              (@ApplicationModule + allowedDependencies)

src/main/resources/db/migration/
└── V10__promotion_tables.sql

# ordering module — modifications
src/main/java/com/cardapio/ordering/domain/model/Cart.java                  (add couponCode)
src/main/java/com/cardapio/ordering/domain/model/Order.java                 (add appliedCouponCode + populate discount)
src/main/java/com/cardapio/ordering/domain/event/OrderPlaced.java           (add couponCode field)
src/main/java/com/cardapio/ordering/domain/event/OrderCanceled.java         (add couponCode field)
src/main/java/com/cardapio/ordering/application/command/PlaceOrderCommand.java   (carry coupon)
src/main/java/com/cardapio/ordering/application/usecase/PlaceOrderUseCase.java   (re-evaluate + apply discount + emit code on event)
src/main/java/com/cardapio/ordering/application/usecase/CancelOrderUseCase.java  (emit code on cancel event)
src/main/java/com/cardapio/ordering/application/usecase/ApplyCouponUseCase.java  (NEW: cart-level)
src/main/java/com/cardapio/ordering/application/usecase/RemoveCouponUseCase.java (NEW)
src/main/java/com/cardapio/ordering/api/rest/CartController.java            (PATCH/DELETE coupon)
src/main/java/com/cardapio/ordering/api/dto/CartResponse.java               (expose discount preview)
src/main/java/com/cardapio/ordering/infrastructure/persistence/jpa/CartJpaEntity.java (add coupon_code)
src/main/java/com/cardapio/ordering/infrastructure/persistence/jpa/OrderJpaEntity.java (add applied_coupon_code)
src/main/java/com/cardapio/ordering/infrastructure/persistence/mapper/CartMapper.java
src/main/java/com/cardapio/ordering/infrastructure/persistence/mapper/OrderMapper.java
src/main/java/com/cardapio/ordering/package-info.java                       (allowedDependencies += promotion::CouponQueryPort, promotion::ids, promotion::dto)
```

---

## Task 1: `promotion` module skeleton + value objects

**Files:** `package-info.java` (module), `CouponId`, `CouponCode`, `DiscountType`

- [ ] **Step 1.1:** `promotion/package-info.java` declares the module:
```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Promotion",
    allowedDependencies = {"shared", "api::error", "api::support", "ordering::events", "ordering::ids"}
)
package com.cardapio.promotion;
```
- [ ] **Step 1.2:** `CouponId` record-VO mirroring previous IDs.
- [ ] **Step 1.3:** `CouponCode` record-VO. `code = code.trim().toUpperCase(Locale.ROOT)`; reject blank, non-`[A-Z0-9-]`, length > 32. `equals` is on the canonical form.
- [ ] **Step 1.4:** `DiscountType { PERCENT, FIXED }`.
- [ ] **Step 1.5:** `domain/model/package-info.java` → `@NamedInterface("ids")` (so `ordering` can refer to `CouponId` if ever needed).
- [ ] **Step 1.6:** Tests for `CouponCode` normalization (lowercase becomes uppercase; `"  promo-1 "` → `"PROMO-1"`; `"!"` rejected).
- [ ] **Step 1.7:** Modulith verifier: `./mvnw test -Dtest=ModulithVerificationTest`.
- [ ] **Step 1.8:** Commit: `feat(promotion): add module skeleton and value objects`.

---

## Task 2: `Coupon` aggregate + tests

**Files:** `Coupon.java`, `CouponInvariantException.java`, `CouponTest.java`

- [ ] **Step 2.1:** Factory `Coupon.create(code, type, value, validFrom, validUntil, minOrderValue?, maxUses?, currency, clock)`:
  - `value > 0`. `PERCENT` requires `0 < value <= 100`. `FIXED` requires same currency as `minOrderValue`.
  - `validUntil > validFrom` if both present; either may be `null` (unbounded).
  - `usesCount = 0`, `active = true`.
- [ ] **Step 2.2:** Read-only methods: `isActiveAt(Instant now)`, `withinDateWindow(Instant now)`, `isExhausted()`, `meetsMinOrder(Money subtotal)`.
- [ ] **Step 2.3:** Pricing: `discountFor(Money subtotal): Money`:
  - `PERCENT`: `subtotal * value / 100`, bankers-round to currency scale.
  - `FIXED`: `min(value, subtotal)` (never exceeds subtotal).
- [ ] **Step 2.4:** Mutators (admin-only): `update(value?, validUntil?, minOrderValue?, maxUses?, clock)`, `deactivate(clock)`, `activate(clock)`.
- [ ] **Step 2.5:** Lifecycle: `incrementUses(clock)` — throws `CouponInvariantException` if `isExhausted()`. `decrementUses(clock)` — clamps at 0 (defensive; idempotency lives at the use case via `coupon_uses` PK, but the aggregate should not crash on a duplicate decrement event).
- [ ] **Step 2.6:** Tests:
  - PERCENT 10% on 100 → 10.
  - FIXED 25 on 10 → 10 (clamped).
  - PERCENT > 100 rejected; FIXED in different currency rejected.
  - `incrementUses` blocks at `maxUses`.
- [ ] **Step 2.7:** Commit.

---

## Task 3: JPA entity + repository adapter + Flyway V10

**Files:** `CouponJpaEntity`, `CouponUseJpaEntity`, repos, adapter, `V10__promotion_tables.sql`

- [ ] **Step 3.1:** `coupons` columns: `id UUID PK`, `code VARCHAR(32) UNIQUE NOT NULL`, `discount_type VARCHAR(10)`, `value NUMERIC(12,2)`, `currency VARCHAR(3)`, `valid_from TIMESTAMPTZ NULL`, `valid_until TIMESTAMPTZ NULL`, `min_order_value NUMERIC(12,2) NULL`, `max_uses INT NULL`, `uses_count INT NOT NULL DEFAULT 0`, `active BOOLEAN NOT NULL`, `created_at`, `updated_at`.
- [ ] **Step 3.2:** `coupon_uses` (idempotency): `coupon_code VARCHAR(32)`, `order_id UUID`, `applied_at TIMESTAMPTZ`, `PK (coupon_code, order_id)`. No FK to `orders` (cross-schema rule). `coupon_code` denormalized vs. `coupon_id` so that admin can rotate/delete a coupon and we keep an audit trail.
- [ ] **Step 3.3:** Spring Data repos with finders `findByCode`, `findAllByOrderByCreatedAtDesc`. `coupon_uses`: `existsByCouponCodeAndOrderId`, `save`.
- [ ] **Step 3.4:** Mapper + adapter implementing `CouponRepository`.
- [ ] **Step 3.5:** Adapter integration test (Testcontainers): roundtrip + `incrementUses` survives reload + unique-code constraint.
- [ ] **Step 3.6:** Commit.

---

## Task 4: `CouponEvaluation` + `EvaluateCouponUseCase` (impl of `CouponQueryPort`)

**Files:** `CouponEvaluation.java`, `CouponQueryPort.java`, `EvaluateCouponUseCase.java`, tests

- [ ] **Step 4.1:** `CouponEvaluation` sealed interface with cases:
```java
record Applicable(CouponCode code, Money discount, Money subtotal) implements CouponEvaluation {}
record NotFound(CouponCode code) implements CouponEvaluation {}
record Inactive(CouponCode code) implements CouponEvaluation {}
record Expired(CouponCode code) implements CouponEvaluation {}
record BelowMinOrder(CouponCode code, Money minOrder, Money subtotal) implements CouponEvaluation {}
record Exhausted(CouponCode code) implements CouponEvaluation {}
```
- [ ] **Step 4.2:** `CouponQueryPort.evaluate(CouponCode, Money subtotal): CouponEvaluation` exposed via `@NamedInterface("CouponQueryPort")`.
- [ ] **Step 4.3:** `EvaluateCouponUseCase` implements port:
  1. `findByCode` → if absent → `NotFound`.
  2. `!active` → `Inactive`.
  3. `!withinDateWindow(now)` → `Expired`.
  4. `isExhausted()` → `Exhausted`.
  5. `!meetsMinOrder(subtotal)` → `BelowMinOrder`.
  6. else `Applicable(code, coupon.discountFor(subtotal), subtotal)`.
- [ ] **Step 4.4:** Tests for each branch.
- [ ] **Step 4.5:** Commit.

---

## Task 5: Admin CRUD + facade + REST + tests

**Files:** `PromotionFacade`, impl, use cases (Create/Update/Deactivate/List), `AdminCouponController`, request/response DTOs, error codes, tests

- [ ] **Step 5.1:** Add `ErrorCode` entries: `COUPON_NOT_FOUND`, `COUPON_CODE_TAKEN`, `INVALID_COUPON`, `COUPON_INACTIVE`, `COUPON_EXPIRED`, `COUPON_BELOW_MIN_ORDER`, `COUPON_EXHAUSTED`.
- [ ] **Step 5.2:** `CreateCouponUseCase`: rejects duplicate code; persists; returns `Result<CouponView>`.
- [ ] **Step 5.3:** `UpdateCouponUseCase`: load → `coupon.update(...)` → save. **Does not** touch `usesCount`.
- [ ] **Step 5.4:** `DeactivateCouponUseCase`: `coupon.deactivate(clock)`.
- [ ] **Step 5.5:** `ListCouponsUseCase` returns paged list (admin filters: `activeOnly`, `code` prefix). Pagination via `limit/offset` like the other admin endpoints.
- [ ] **Step 5.6:** `AdminCouponController` at `/api/v1/admin/coupons` (`hasAnyRole('OWNER','MANAGER')`; `OPERATOR` is excluded — coupons are commercial decisions).
- [ ] **Step 5.7:** OpenAPI/javadoc on endpoints + request validation (`@NotBlank`, `@Min`, `@Future` etc).
- [ ] **Step 5.8:** Use-case unit tests + MockMvc tests.
- [ ] **Step 5.9:** Commit.

---

## Task 6: `Cart` apply/remove coupon — ordering side

**Files:** `Cart.java` (modify), `CartJpaEntity` + mapper (modify), `ApplyCouponUseCase`, `RemoveCouponUseCase`, `CartController` (modify), `CartView` (modify), tests

- [ ] **Step 6.1:** `Cart` adds `couponCode: Optional<String>` + `applyCoupon(String code, Clock)` / `removeCoupon(Clock)`. Pure state — does not validate; validation lives in the use case.
- [ ] **Step 6.2:** Flyway delta: `ALTER TABLE carts ADD COLUMN coupon_code VARCHAR(32)`. Place this in V10 alongside the promotion tables to keep the migration self-contained, OR a new V11 if V10 has already been applied. Pick **V10 includes the cart column** since this is a single feature.
- [ ] **Step 6.3:** `ApplyCouponUseCase.execute(customerId, rawCode)`:
  1. Load cart.
  2. Compute subtotal via `CartPricingService` (already exists — just call it).
  3. Call `CouponQueryPort.evaluate(CouponCode.of(rawCode), subtotal)`.
  4. Map non-`Applicable` cases to `Result.failWith(corresponding ErrorCode)` with the coupon-specific message.
  5. On `Applicable`: `cart.applyCoupon(code, clock)`; save; return new `CartView` with discount preview.
- [ ] **Step 6.4:** `RemoveCouponUseCase.execute(customerId)`: load → `cart.removeCoupon` → save → return cart.
- [ ] **Step 6.5:** `CartController` adds `PATCH /api/v1/cart/coupon { code }` and `DELETE /api/v1/cart/coupon`.
- [ ] **Step 6.6:** `CartView` / `CartResponse` carry `appliedCouponCode`, `discount`, `discountedTotal` so the cart screen can render before checkout.
- [ ] **Step 6.7:** Tests: apply happy path; apply rejects expired; apply replaces previous code (PATCH semantics); remove on empty cart is no-op.
- [ ] **Step 6.8:** Commit.

---

## Task 7: Apply discount at checkout — `PlaceOrderUseCase`

**Files:** `PlaceOrderCommand` (modify? — coupon comes from `Cart`, not from request body — confirm), `PlaceOrderUseCase` (modify), `Order.java` (modify), `OrderJpaEntity` + mapper (modify), tests

- [ ] **Step 7.1:** Decision: **coupon code is read from the `Cart`** at checkout, not from the request body. This avoids a desync between "what the user sees in the cart" and "what gets applied". `PlaceOrderRequest`/`PlaceOrderCommand` are unchanged for coupons.
- [ ] **Step 7.2:** `Order` adds `appliedCouponCode: Optional<String>` + persisted column `applied_coupon_code`. The Money `discount` field already exists (defaulted to zero) — start populating it.
- [ ] **Step 7.3:** `Order.place(...)` overload accepting `Money discount` and `Optional<String> couponCode`. Old overloads delegate with zero discount and empty code, mirroring the Phase 5 invariants pattern. Total recomputation: `total = subtotal + deliveryFee - discount` with `discount <= subtotal` invariant (never negative total).
- [ ] **Step 7.4:** In `PlaceOrderUseCase`, after pricing, before building the Order:
  1. If `cart.couponCode().isPresent()`: re-evaluate via `CouponQueryPort.evaluate(...)`.
     - On non-`Applicable`: return `Result.failWith(corresponding ErrorCode)` and clear the cart's coupon (the user has a stale coupon — surface it explicitly so they retry).
     - On `Applicable`: capture the discount Money + code.
  2. Else: discount = ZERO, no code.
- [ ] **Step 7.5:** Build the Order with the discount + coupon code.
- [ ] **Step 7.6:** After persistence, publish `OrderPlaced` with the new `couponCode` field set when present.
- [ ] **Step 7.7:** Test cases:
  - Cart with valid PERCENT coupon → Order.discount populated, total reduced.
  - Cart with FIXED > subtotal → discount clamped to subtotal, total = deliveryFee (or 0 for DINE_IN/PICKUP).
  - Cart with expired coupon → place fails with `COUPON_EXPIRED`; cart's coupon cleared.
  - Cart without coupon → unchanged behavior.
- [ ] **Step 7.8:** Commit.

---

## Task 8: `OrderPlaced` / `OrderCanceled` events carry coupon code

**Files:** `OrderPlaced.java` (modify), `OrderCanceled.java` (modify), `CancelOrderUseCase.java` (modify), Phase-4 listeners (audit only — no behavior change), tests

- [ ] **Step 8.1:** Add `Optional<String> couponCode` (or nullable `String`) to both event records. Default `of(...)` factory keeps existing call sites working (overload).
- [ ] **Step 8.2:** `PlaceOrderUseCase` uses the new factory variant when emitting.
- [ ] **Step 8.3:** `CancelOrderUseCase`: read `Order.appliedCouponCode()` and include it on the published `OrderCanceled`.
- [ ] **Step 8.4:** Verify Phase 4 notification listeners still compile (they don't read the new field but the event type changed).
- [ ] **Step 8.5:** Existing event-construction tests updated.
- [ ] **Step 8.6:** Commit.

---

## Task 9: `promotion` listeners — increment / decrement `usesCount`

**Files:** `OrderPlacedCouponListener.java`, `OrderCanceledCouponListener.java`, `CouponUseRepository`, tests

- [ ] **Step 9.1:** `OrderPlacedCouponListener.on(OrderPlaced event)`:
  - Skip if `event.couponCode()` is null/blank.
  - Look up coupon by code; if absent (deleted between place and event delivery) → log + skip.
  - Idempotency: if `coupon_uses` already has `(code, orderId)` → skip.
  - Otherwise: `coupon.incrementUses(clock)`; save; insert `coupon_uses` row. Single TX.
- [ ] **Step 9.2:** `OrderCanceledCouponListener.on(OrderCanceled event)`:
  - Skip if `event.couponCode()` is null/blank.
  - Idempotency: if `coupon_uses` does NOT have `(code, orderId)` → skip (we never counted it; nothing to undo).
  - Otherwise: `coupon.decrementUses(clock)`; save; delete `coupon_uses` row.
- [ ] **Step 9.3:** Both methods annotated `@org.springframework.modulith.events.ApplicationModuleListener` (transactional + async, retried via the Modulith outbox).
- [ ] **Step 9.4:** Tests with `@ApplicationModuleTest`:
  - Publish `OrderPlaced(couponCode=X)` → assert `usesCount == 1` and `coupon_uses` has the row.
  - Publish twice with same orderId → still 1 (idempotent).
  - Publish `OrderCanceled(couponCode=X)` → 0.
  - Cancel without prior place → no change (defensive).
- [ ] **Step 9.5:** Commit.

---

## Task 10: Configuration + module dependencies

**Files:** `ordering/package-info.java` (modify), `application.yml` (no changes expected), `ErrorCode` (already from Task 5)

- [ ] **Step 10.1:** Update `ordering/package-info.java`:
  ```java
  allowedDependencies = {
      ... existing ...,
      "promotion::CouponQueryPort",
      "promotion::ids",
      "promotion::dto"
  }
  ```
- [ ] **Step 10.2:** Run `./mvnw test -Dtest=ModulithVerificationTest`.
- [ ] **Step 10.3:** Commit (likely small standalone commit alongside Tasks).

---

## Task 11: Bruno collection — coupons

**Files:** `bruno/15_Admin Coupons/*.bru`, additions to `bruno/06_Customer Cart/`

- [ ] **Step 11.1:** Admin: Create / Update / Deactivate / List + duplicate-code error case.
- [ ] **Step 11.2:** Customer: Apply / Remove coupon on cart; Place Order with coupon (single happy path); error scenarios (expired, below min, exhausted).
- [ ] **Step 11.3:** Each admin request stores `couponCode` to env var; customer requests reuse it.
- [ ] **Step 11.4:** Commit.

---

## Task 12: Modulith verification + integration test

- [ ] **Step 12.1:** `./mvnw test -Dtest=ModulithVerificationTest`.
- [ ] **Step 12.2:** New `PromotionFlowIT`:
  1. Admin creates `PERCENT 10%` coupon.
  2. Customer puts items in cart, `PATCH /cart/coupon` → 200 with discount preview.
  3. Place order → 201, `Order.discount > 0`, `total < subtotal + fee`.
  4. Await listener: `usesCount` becomes 1.
  5. Cancel order → await listener: `usesCount` back to 0.
- [ ] **Step 12.3:** Commit.

---

## Task 13: Manual smoke

- [ ] **Step 13.1:** `mvn spring-boot:run`. Through Bruno: create `WELCOME10`, apply on a 50.00 cart, place order, observe DB: `coupons.uses_count = 1`, `orders.applied_coupon_code = 'WELCOME10'`, `orders.discount = 5.00`.
- [ ] **Step 13.2:** Cancel the order; verify `uses_count = 0`.
- [ ] **Step 13.3:** Try a second coupon `BLACK5` (FIXED 5.00) and confirm replace semantics on the cart (PATCH replaces, doesn't stack).

---

## Done definition

- All checkboxes ticked.
- `./mvnw verify` green.
- Modulith verifier passes with the new module + `ordering` updated allowedDependencies.
- Bruno flows exercise admin + customer paths end-to-end on a running instance.
- Spec coverage: `promotion` aggregate matches §3, event reactions match §5 table.

---

## Risks & open questions

| Item | Risk | Mitigation |
|---|---|---|
| Cart coupon goes stale between apply and place | Customer expects N% off, gets less or fails at checkout | Re-evaluate at place; on failure clear cart's coupon and surface error so user retries |
| Race: two orders use the same `maxUses=1` coupon | Could overshoot `usesCount` | Listener uses pessimistic recheck inside the same TX (`select … for update`); defensive throw in `incrementUses` aborts second TX. Acceptable for MVP scale. |
| Coupon deletion vs. open orders | Order references a code that no longer exists | `coupon_uses` is denormalized on `code`; deleting a coupon stops new uses but historical rows survive. Admin "deactivate" preferred over "delete". |
| Listener TX failures | `usesCount` never gets incremented | Modulith event publication outbox retries; failed events stay in the outbox table with a non-null error reason for manual replay. |
| Floating-point in PERCENT discount | Sub-cent rounding mismatches | Use `BigDecimal` with bankers rounding to currency scale (already the convention in `Money`). |
| `OrderPlaced` listener in `notification` module needs the new field | Compilation breaks | Field is additive (`Optional<String>`); existing listeners ignore it. Verify in CI. |
| Coupon code case sensitivity | Customer types `welcome10`, admin created `WELCOME10` | `CouponCode` VO normalizes to uppercase + trims; both apply paths run through the VO. |
