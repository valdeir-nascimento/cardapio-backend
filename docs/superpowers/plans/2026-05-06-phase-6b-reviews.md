# Phase 6b — Reviews (Customer Order Ratings) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the `review` bounded context. After an `Order` reaches a terminal-success state (`DELIVERED`, `PICKED_UP`, or `SERVED`), the customer can submit one rating (1-5 stars) plus an optional comment. Admin sees aggregate stats per product (and per order). The module reacts to `OrderStatusChanged` to mark orders as eligible-to-review (so the cart screen / "my orders" can render a "rate now" CTA), then accepts the customer's submission via REST.

**Architecture:** Clean Architecture inside `com.cardapio.review`.
- Aggregate: `Review` (id, orderId, customerId, rating, comment, createdAt). One review per (orderId, customerId) — DB unique index enforces.
- Read-side projection: `ReviewableOrder` (orderId, customerId, modality, productIds[], terminalAt) materialized from `OrderStatusChanged` events. The customer flow reads this to:
  1. Know if a given order is eligible (`canReview`).
  2. Tell the customer which products are part of that order without `review` having to call `ordering`'s public API at submission time. Decision: **embed productIds in the projection** so the rating writes against denormalized data and stays purely event-sourced from this side.
- Use cases return `Result<T>` with Notification.
- Admin queries are projection-backed (`AVG(rating)`, `COUNT(*)`) — no on-the-fly aggregations.

**Cross-context contracts:**
- Listener `@ApplicationModuleListener on(OrderStatusChanged)`:
  - On `RECEIVED → CONFIRMED → ...` transitions, the listener marks the projection: when status hits a terminal-success state, `ReviewableOrder` is upserted with `terminalAt = event.occurredOn`.
  - On `→ CANCELED`, project as not-reviewable (delete or mark with a flag — pick: delete the projection row to keep the table small).
- Need product breakdown on the order. Options:
  1. Project from `OrderPlaced` (which already runs through Phase 5 ordering listeners) — but `OrderPlaced` does **not** carry items. Either extend the event (additive, but pollutes the contract) or call back to ordering at projection time.
  2. Listener calls a new `ordering::OrderingFacade` method (e.g., `getOrderProductIds(orderId)`).
  - **Pick option 2.** `OrderingFacade` is already a NamedInterface and adding a read method is cheaper than carrying items on every event.
- `review` reads customer name via existing `IdentityFacade.getCustomerContact(...)` (already exposed by Phase 4).
- `review` does **not** publish events in this phase.

**Tech Stack additions:** none.

**Reference:**
- Spec: [docs/superpowers/specs/2026-05-04-cardapio-digital-backend-design.md](../specs/2026-05-04-cardapio-digital-backend-design.md) — §2 contexts, §3 review, §5 events table (`OrderStatusChanged → review`)
- Phase 4 plan: [docs/superpowers/plans/2026-05-06-phase-4-notifications.md](./2026-05-06-phase-4-notifications.md) — `@ApplicationModuleListener` and `IdentityFacade` exposure
- Phase 6a plan: [docs/superpowers/plans/2026-05-06-phase-6a-promotions.md](./2026-05-06-phase-6a-promotions.md) — `coupon_uses` ledger pattern (we'll mirror the upsert idempotency)

**Out of scope (deferred):**
- Photo attachments on reviews — text + stars only for MVP.
- Helpful / Not-helpful voting on other customers' reviews — Phase 7+.
- Review moderation queue (admin approve/reject) — Phase 7.
- Review reply by admin — Phase 7.
- Anonymous reviews — every review is tied to the customer subject.
- Per-add-on rating — rating is whole-order; UI can show it on each product line.
- Review reminders / nudges via notification — could come later as another notification listener observing `ReviewableOrder` rows older than X days.

---

## File Structure

```
src/main/java/com/cardapio/review/
├── domain/
│   ├── model/
│   │   ├── ReviewId.java                          (record-VO)
│   │   ├── Rating.java                            (VO; 1..5 inclusive)
│   │   ├── Comment.java                           (VO; max 500 chars, trims)
│   │   ├── Review.java                            (aggregate)
│   │   ├── ReviewableOrder.java                   (read-side projection)
│   │   └── package-info.java                      (@NamedInterface("ids"))
│   ├── port/
│   │   ├── ReviewRepository.java
│   │   └── ReviewableOrderRepository.java
│   └── exception/
│       └── ReviewInvariantException.java
├── application/
│   ├── command/
│   │   └── SubmitReviewCommand.java
│   ├── usecase/
│   │   ├── SubmitReviewUseCase.java
│   │   ├── GetMyReviewableOrdersUseCase.java
│   │   ├── GetMyReviewUseCase.java
│   │   └── ProductRatingStatsUseCase.java         (admin)
│   ├── event/
│   │   └── OrderStatusChangedReviewListener.java  (upserts/deletes ReviewableOrder)
│   ├── dto/
│   │   ├── ReviewView.java
│   │   ├── ReviewableOrderView.java
│   │   ├── ProductRatingStatsView.java
│   │   └── package-info.java                      (@NamedInterface("dto"))
│   └── ReviewFacade.java + Impl                   (admin + customer surface)
├── infrastructure/
│   ├── persistence/
│   │   ├── jpa/
│   │   │   ├── ReviewJpaEntity.java
│   │   │   └── ReviewableOrderJpaEntity.java      (@ElementCollection on product_ids
│   │   │                                           or join table review_able_order_products)
│   │   ├── repository/
│   │   │   ├── SpringReviewJpaRepository.java
│   │   │   └── SpringReviewableOrderJpaRepository.java
│   │   ├── mapper/
│   │   │   ├── ReviewMapper.java
│   │   │   └── ReviewableOrderMapper.java
│   │   └── adapter/
│   │       ├── ReviewRepositoryAdapter.java
│   │       └── ReviewableOrderRepositoryAdapter.java
│   └── ordering/
│       └── OrderingProductLookupAdapter.java      (calls OrderingFacade.getOrderProductIds)
├── api/
│   ├── rest/
│   │   ├── CustomerReviewController.java
│   │   └── AdminReviewStatsController.java
│   └── dto/
│       ├── SubmitReviewRequest.java
│       ├── ReviewResponse.java
│       └── ReviewableOrderResponse.java
└── package-info.java                              (@ApplicationModule + allowedDependencies)

src/main/resources/db/migration/
└── V11__review_tables.sql

# ordering module — modifications
src/main/java/com/cardapio/ordering/application/OrderingFacade.java       (add getOrderProductIds(OrderId))
src/main/java/com/cardapio/ordering/application/OrderingFacadeImpl.java   (impl)
```

---

## Task 1: `review` module skeleton + value objects

**Files:** `package-info.java`, `ReviewId`, `Rating`, `Comment`, `domain/model/package-info.java`

- [ ] **Step 1.1:** `package-info.java`:
```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Review",
    allowedDependencies = {
        "shared",
        "api::error",
        "api::support",
        "ordering::events",
        "ordering::ids",
        "ordering::OrderingFacade",
        "ordering::dto",
        "identity::IdentityFacade",
        "identity::dto"
    }
)
package com.cardapio.review;
```
- [ ] **Step 1.2:** `ReviewId` record-VO.
- [ ] **Step 1.3:** `Rating` record-VO (`int stars`); rejects `< 1 || > 5`.
- [ ] **Step 1.4:** `Comment` record-VO; trims, allows null/blank → empty, rejects length > 500 after trim.
- [ ] **Step 1.5:** `domain/model/package-info.java` → `@NamedInterface("ids")`.
- [ ] **Step 1.6:** Tests for VOs.
- [ ] **Step 1.7:** Modulith verifier: `./mvnw test -Dtest=ModulithVerificationTest`.
- [ ] **Step 1.8:** Commit: `feat(review): add module skeleton and value objects`.

---

## Task 2: `Review` aggregate + `ReviewableOrder` projection + tests

**Files:** `Review.java`, `ReviewableOrder.java`, `ReviewInvariantException.java`, tests

- [ ] **Step 2.1:** `Review.create(orderId, customerId, rating, comment, clock)` factory; `update(...)` is **not** offered — reviews are immutable in MVP (admin can delete via Phase 7 moderation).
- [ ] **Step 2.2:** `Review` does **not** validate "is the order in a terminal state?" — that's the use case's job (it consults `ReviewableOrder`).
- [ ] **Step 2.3:** `ReviewableOrder` is a small read-side aggregate:
  ```
  orderId (PK), customerId, modality, productIds[], terminalAt
  ```
  - Factory `ReviewableOrder.materialize(orderId, customerId, modality, productIds, terminalAt)`.
  - Pure data — no behavior.
- [ ] **Step 2.4:** Tests: factory invariants for `Review`; `Rating` 0 and 6 rejected.
- [ ] **Step 2.5:** Commit.

---

## Task 3: JPA entities + Flyway V11 + repo adapters

**Files:** `ReviewJpaEntity`, `ReviewableOrderJpaEntity`, mappers, adapters, `V11__review_tables.sql`

- [ ] **Step 3.1:** `reviews` table — `id UUID PK`, `order_id UUID NOT NULL`, `customer_id UUID NOT NULL`, `rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5)`, `comment VARCHAR(500)`, `created_at TIMESTAMPTZ`. Unique index on `(order_id, customer_id)`. Soft refs only (no FK cross-schema).
- [ ] **Step 3.2:** `reviewable_orders` table — `order_id UUID PK`, `customer_id UUID NOT NULL`, `modality VARCHAR(16) NOT NULL`, `terminal_at TIMESTAMPTZ NOT NULL`, `created_at`. Plus child table `reviewable_order_products(order_id, product_id, position)` — composite PK.
- [ ] **Step 3.3:** Indexes: `idx_reviews_product_id` (no — reviews don't carry product_id directly; rating-per-product comes from joining via reviewable_order_products). `idx_reviewable_orders_customer ON reviewable_orders(customer_id, terminal_at DESC)` for the "my reviewable orders" listing.
- [ ] **Step 3.4:** JPA entities. `ReviewableOrderJpaEntity` uses `@ElementCollection` of UUIDs on a join table (mirrors `Comanda` pattern from Phase 5).
- [ ] **Step 3.5:** Spring Data repos and mappers.
- [ ] **Step 3.6:** Adapters implement domain ports.
- [ ] **Step 3.7:** Persistence IT (Testcontainers): roundtrip review (with comment + without); duplicate `(orderId, customerId)` insert fails with `DataIntegrityViolationException`; `ReviewableOrder` roundtrip preserves productIds order.
- [ ] **Step 3.8:** Commit.

---

## Task 4: `OrderingFacade.getOrderProductIds(orderId)` exposure

**Files:** `OrderingFacade.java`, `OrderingFacadeImpl.java` (modify)

- [ ] **Step 4.1:** Add `List<UUID> getOrderProductIds(OrderId orderId)` to the interface. The contract: returns the list of distinct product IDs in the order's items, in original line order. If the order doesn't exist → `NotFoundException`.
- [ ] **Step 4.2:** Implementation reuses `getOrder(adminContext)` internally and pulls items.
- [ ] **Step 4.3:** Unit test for the new facade method (mock OrderRepository).
- [ ] **Step 4.4:** Commit.

---

## Task 5: `OrderStatusChangedReviewListener` projection updates

**Files:** `OrderStatusChangedReviewListener.java`, tests

- [ ] **Step 5.1:** Listener reacts to `OrderStatusChanged`. Pattern: switch on `event.toStatus()`:
  - `DELIVERED | PICKED_UP | SERVED` → upsert a `ReviewableOrder` row. Look up productIds via `OrderingFacade.getOrderProductIds(...)`. terminalAt = `event.occurredOn()`.
  - `CANCELED` → delete the projection row if present (covers the rare race where an order somehow flips terminal-then-canceled; defensive).
  - other → no-op.
- [ ] **Step 5.2:** Idempotent: upsert by `orderId` PK; if event is delivered twice the row is just rewritten with the same content.
- [ ] **Step 5.3:** Use `@org.springframework.modulith.events.ApplicationModuleListener` (transactional + async, automatic outbox).
- [ ] **Step 5.4:** Tests with `@ApplicationModuleTest` or `TransactionTemplate`-publish pattern from Phase 6a:
  - PICKUP order → `PREPARING → READY → PICKED_UP` triggers projection.
  - CANCEL after projection → projection cleared.
  - Duplicate `OrderStatusChanged(PICKED_UP)` → still one row.
- [ ] **Step 5.5:** Commit.

---

## Task 6: `SubmitReviewUseCase` + customer endpoints

**Files:** `SubmitReviewCommand`, `SubmitReviewUseCase`, `CustomerReviewController`, request/response DTOs, error codes

- [ ] **Step 6.1:** Add `ErrorCode` entries: `ORDER_NOT_REVIEWABLE`, `REVIEW_ALREADY_SUBMITTED`, `INVALID_RATING`, `INVALID_COMMENT`.
- [ ] **Step 6.2:** `SubmitReviewCommand(orderId, customerId, rating, comment)`.
- [ ] **Step 6.3:** Use case:
  1. Look up `ReviewableOrder` by `orderId`. If absent → `ORDER_NOT_REVIEWABLE`.
  2. Assert `reviewableOrder.customerId().equals(customerId)`. If not → `ORDER_NOT_REVIEWABLE` (don't leak existence to other users).
  3. Reject if a `Review` already exists for `(orderId, customerId)` → `REVIEW_ALREADY_SUBMITTED`.
  4. Build `Review.create(...)`, persist, return view.
  5. Race: two concurrent submissions could both pass the existence check then collide on the unique index. Catch `DataIntegrityViolationException` and surface as `REVIEW_ALREADY_SUBMITTED` (not 500).
- [ ] **Step 6.4:** `GetMyReviewableOrdersUseCase` returns paged list of `ReviewableOrder` rows for the authenticated customer that don't yet have a review (LEFT JOIN at the SQL level; do this with a custom finder, not in-memory).
- [ ] **Step 6.5:** `GetMyReviewUseCase` returns the customer's review for an order (or 404).
- [ ] **Step 6.6:** Controller `/api/v1/me/reviewable-orders` (GET), `/api/v1/orders/{id}/review` (POST submit, GET retrieve). Customer JWT required.
- [ ] **Step 6.7:** Tests: use-case unit tests for each branch + MockMvc on the controller.
- [ ] **Step 6.8:** Commit.

---

## Task 7: Admin product rating stats

**Files:** `ProductRatingStatsUseCase`, `AdminReviewStatsController`, `ProductRatingStatsView`

- [ ] **Step 7.1:** Custom JPA query joining `reviews` with `reviewable_order_products` on `order_id` to compute `AVG(rating)`, `COUNT(*)` per `product_id`. Return list ordered by count desc.
- [ ] **Step 7.2:** Endpoint `/api/v1/admin/reviews/stats?productId=...&from=...&to=...` — `productId` optional (returns top N when absent), date range filters by `reviews.created_at`.
- [ ] **Step 7.3:** OWNER + MANAGER role only (OPERATOR is op-floor; stats is commercial).
- [ ] **Step 7.4:** Use-case unit test + an IT that seeds reviews and asserts the aggregate.
- [ ] **Step 7.5:** Commit.

---

## Task 8: `ReviewFacade` + module dependencies

**Files:** `ReviewFacade`, `ReviewFacadeImpl`, controller wiring

- [ ] **Step 8.1:** Facade exposes the customer + admin operations consistently with `OrderingFacade` style (NotificationException / NotFoundException at the boundary).
- [ ] **Step 8.2:** Confirm `package-info.java` allowedDependencies covers everything actually imported (Modulith verifier will tell us).
- [ ] **Step 8.3:** Controllers inject the facade (not raw use cases), to keep parity with other contexts.
- [ ] **Step 8.4:** Commit.

---

## Task 9: Bruno collection — reviews

**Files:** `bruno/16_Reviews/*.bru`

- [ ] **Step 9.1:** Customer: list reviewable orders, submit review (happy path), submit again (error: `REVIEW_ALREADY_SUBMITTED`), submit on non-terminal order (error: `ORDER_NOT_REVIEWABLE`), get my review.
- [ ] **Step 9.2:** Admin: list product rating stats overall and filtered by product.
- [ ] **Step 9.3:** Each request stores `reviewId` to env where useful.
- [ ] **Step 9.4:** Commit.

---

## Task 10: Modulith verification + integration test

- [ ] **Step 10.1:** `./mvnw test -Dtest=ModulithVerificationTest`.
- [ ] **Step 10.2:** New `ReviewFlowIT`:
  1. Place a PICKUP order through `OrderingFacade` (or a test fixture seeded directly into the DB).
  2. Publish `OrderStatusChanged(... → PICKED_UP)` via `TransactionTemplate`.
  3. Await projection via Awaitility.
  4. Submit a review through the use case.
  5. Assert the review is persisted and that a second submission fails with `REVIEW_ALREADY_SUBMITTED`.
  6. Publish `OrderStatusChanged(... → CANCELED)` after the fact (defensive); assert review is **not** removed (history is preserved) but projection row is cleared.
- [ ] **Step 10.3:** Commit.

---

## Task 11: Manual smoke

- [ ] **Step 11.1:** `mvn spring-boot:run`. Bruno: place order → admin advances to PICKED_UP → customer lists `/me/reviewable-orders` and gets the row → submits review → tries to submit again (fails) → admin queries product stats and sees the average.
- [ ] **Step 11.2:** Verify DB: `reviews` has the row, `reviewable_orders` has the projection (still present even after review since we don't auto-clean — we let the listing query LEFT JOIN exclude already-rated ones).

---

## Done definition

- All checkboxes ticked.
- `./mvnw verify` green (unit + integration + Modulith verifier).
- Bruno flows exercise customer + admin paths on a running instance.
- Spec coverage: `Review` aggregate matches §3, `OrderStatusChanged → review` reaction matches §5.

---

## Risks & open questions

| Item | Risk | Mitigation |
|---|---|---|
| Listener fails to fetch productIds (ordering down / network blip) | Projection row never lands → user can't review | Modulith outbox retries the event automatically. If still failing, the event sits in `event_publication` until manual replay (Phase 7 ops). |
| Race: two concurrent review submissions | Both pass existence check, both insert | Unique index + catch `DataIntegrityViolationException` → return typed error. |
| Review for canceled order | UI lets the customer rate a CANCELED order | Listener deletes the projection on `→ CANCELED`; SubmitReviewUseCase checks projection first. |
| Stats query is expensive on growing tables | Slow admin dashboard | Index on `reviewable_order_products(product_id)`; for MVP, table size is small. Phase 7 adds a materialized view if needed. |
| Customer name PII in admin stats endpoint | LGPD concern | Stats endpoint returns aggregates only — no `customerId` or comments. The "list all reviews of a product" endpoint, if added later, must redact. |
| Comment field used to leak HTML / scripts | XSS in admin dashboard | Comment is stored raw; the front-end is responsible for escaping. Document in API docs. |
| Order has no items somehow | Empty `productIds` projection | Listener tolerates empty list — projection row still created with `productIds = []`; admin stats simply ignore it. |
