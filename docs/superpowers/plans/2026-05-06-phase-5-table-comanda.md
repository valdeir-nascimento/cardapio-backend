# Phase 5 — Mesa + Comanda (DINE_IN) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Habilitar consumo no local. Adicionar dois agregados novos ao módulo `ordering` — `Table` (mesa física com QR code) e `Comanda` (comanda compartilhada entre clientes da mesma mesa) — e estender `Order` para suportar a modalidade `DINE_IN` com workflow `RECEIVED → CONFIRMED → PREPARING → SERVED`. Cliente abre/escaneia QR, abre uma comanda nova ou entra (`join`) numa comanda aberta da mesa, lança seus pedidos sob a comanda; admin fecha a comanda quando todos pagam.

**Architecture:**
- Os dois agregados ficam dentro do módulo `ordering` (não criar módulo novo) — eles compartilham fronteira transacional natural com `Order`. `Comanda` referencia `Order[]` por id (não por entidade JPA cross-aggregate); `Table` é independente.
- QR code é um token opaco UUID gerado no cadastro da mesa (não a URL inteira). A imagem PNG é gerada via **ZXing** e enviada para R2/S3 (Cloudflare R2 prioridade — S3-compatible, custo zero MVP). O endpoint admin retorna **presigned URL** com TTL curto. Substituir o storage é trocar um adapter (`QrStorage` port).
- Admin gera o QR uma vez no cadastro da mesa; o token não rotaciona. Resolução pública: `GET /tables/resolve?token=...` → retorna `{tableId, number, currentComandaId?}` para o front decidir entre "abrir nova comanda" e "entrar na existente".
- Comanda compartilhada: status `OPEN | CLOSED`. `customerIds[]` é a lista de clientes que entraram. `orders[]` agrega ids dos pedidos lançados. Total acumulado é uma **view** computada a partir dos `Order` snapshots — não duplica estado.
- `Order` ganha campos opcionais `tableId`, `comandaId` (ambos nullable; obrigatórios quando `modality = DINE_IN`, proibidos caso contrário — invariante validada na criação).
- Use cases retornam `Result<T>` com Notification para erros de regra; exceções para infra.
- Eventos novos no contrato cross-context: `ComandaOpened`, `ComandaJoined`, `ComandaClosed` (publicados mas sem listener nesta fase — `notification` ganhará reação numa fase futura, fora de escopo aqui).

**Tech Stack additions:**
- `com.google.zxing:core:3.5.3` + `com.google.zxing:javase:3.5.3` — geração PNG do QR code.
- `software.amazon.awssdk:s3:2.x` — cliente S3-compatible. Cloudflare R2 usa endpoint custom + assinatura SigV4 padrão. (Spec sec. 7 já lista R2/S3 como deploy MVP.)
- Reutiliza Resilience4j já presente para o adapter R2.

**Reference:**
- Spec: [docs/superpowers/specs/2026-05-04-cardapio-digital-backend-design.md](../specs/2026-05-04-cardapio-digital-backend-design.md) — §3 `ordering` (Table, Comanda), §4 modality `DINE_IN`, §6 fluxos de mesa, §9 Fase 5
- Phase 2 plan: [docs/superpowers/plans/2026-05-04-phase-2-ordering.md](./2026-05-04-phase-2-ordering.md) — agregado `Order`, status workflow, idempotência
- Phase 4 plan: [docs/superpowers/plans/2026-05-06-phase-4-notifications.md](./2026-05-06-phase-4-notifications.md) — padrão `@NamedInterface`, eventos cross-context

**Out of scope (deferred):**
- Notificação ao admin quando uma comanda abre / fecha — adicionar listener em `notification` na próxima fase de polimento (Fase 6/7).
- Pagamento split por cliente dentro da mesma comanda — MVP fecha a comanda só quando todos os `Order` dela estão `APPROVED`.
- Reabrir comanda fechada — `CLOSED` é terminal nesta fase.
- Bloqueio de mesa (status `OCCUPIED` impedindo nova comanda) — flag derivada (`hasOpenComanda?`); não há transição de estado de `Table` separada.
- Reservas / hora marcada — fora de MVP.
- Migração para outro provedor de storage (S3, MinIO) — port já permite, mas só R2 é implementado.

**Cross-context contract:**
- `Comanda` referencia `customerIds` (UUIDs) — sem FK cross-schema; valida existência via `IdentityFacade.getCustomerContact(...)` (já exposta na Fase 4).
- `Order` continua o agregado central; `Comanda.orders[]` é lista de `OrderId` (soft reference). Não há listener em `notification` nesta fase para os eventos de Comanda.
- Novo `@NamedInterface("dineIn")` exporta os DTOs públicos `TableView`, `ComandaView`, `ComandaSummaryView` para o `api/` consumir.

---

## File Structure

```
src/main/java/com/cardapio/ordering/
├── domain/
│   ├── model/
│   │   ├── TableId.java                          (new VO)
│   │   ├── Table.java                            (new aggregate: id, number, qrToken, qrImageKey, active)
│   │   ├── ComandaId.java                        (new VO)
│   │   ├── Comanda.java                          (new aggregate: id, tableId, customerIds, orderIds, status, openedAt, closedAt)
│   │   ├── ComandaStatus.java                    (enum: OPEN, CLOSED)
│   │   ├── OrderModality.java                    (modify: add DINE_IN)
│   │   ├── OrderStatus.java                      (modify: add SERVED + ALLOWED_DINE_IN map)
│   │   └── Order.java                            (modify: tableId/comandaId nullable fields + invariants)
│   ├── port/
│   │   ├── TableRepository.java                  (new)
│   │   ├── ComandaRepository.java                (new)
│   │   ├── QrCodeGenerator.java                  (new — pure PNG bytes from token)
│   │   └── QrStorage.java                        (new — putIfAbsent(key, bytes), presignedUrl(key, ttl))
│   ├── event/
│   │   ├── ComandaOpened.java                    (new domain event)
│   │   ├── ComandaJoined.java
│   │   └── ComandaClosed.java
│   └── exception/
│       └── DineInInvariantException.java         (new — modality/table/comanda mismatch)
├── application/
│   ├── command/
│   │   ├── CreateTableCommand.java
│   │   ├── ResolveTableTokenCommand.java
│   │   ├── OpenComandaCommand.java
│   │   ├── JoinComandaCommand.java
│   │   ├── CloseComandaCommand.java
│   │   └── PlaceDineInOrderCommand.java          (extends PlaceOrderCommand semantics)
│   ├── usecase/
│   │   ├── admin/
│   │   │   ├── CreateTableUseCase.java
│   │   │   ├── ListTablesUseCase.java
│   │   │   └── CloseComandaUseCase.java
│   │   └── customer/
│   │       ├── ResolveTableTokenUseCase.java
│   │       ├── OpenComandaUseCase.java
│   │       ├── JoinComandaUseCase.java
│   │       └── PlaceDineInOrderUseCase.java
│   └── dto/                                      (@NamedInterface("dineIn") views)
│       ├── TableView.java
│       ├── ComandaView.java
│       └── ComandaSummaryView.java
├── infrastructure/
│   ├── persistence/
│   │   ├── jpa/
│   │   │   ├── TableJpaEntity.java
│   │   │   └── ComandaJpaEntity.java
│   │   ├── repository/
│   │   │   ├── SpringTableJpaRepository.java
│   │   │   └── SpringComandaJpaRepository.java
│   │   ├── mapper/
│   │   │   ├── TableMapper.java
│   │   │   └── ComandaMapper.java
│   │   └── adapter/
│   │       ├── TableRepositoryAdapter.java
│   │       └── ComandaRepositoryAdapter.java
│   ├── qr/
│   │   ├── ZxingQrCodeGenerator.java             (impl QrCodeGenerator)
│   │   ├── R2Properties.java
│   │   ├── R2Config.java                         (S3Client bean apontando para R2)
│   │   └── R2QrStorage.java                      (impl QrStorage, presigned URLs)
│   └── ordering/
│       └── (existing — só recebe cargas dos JPA já modificados)
└── api/
    ├── rest/
    │   ├── admin/
    │   │   ├── AdminTableController.java         (POST /admin/tables, GET /admin/tables, GET /admin/tables/{id}/qr)
    │   │   └── AdminComandaController.java       (POST /admin/comandas/{id}/close, GET /admin/comandas?status=OPEN)
    │   └── customer/
    │       ├── TableResolveController.java       (GET /tables/resolve?token=...)
    │       └── ComandaController.java            (POST /comandas, POST /comandas/{id}/join, GET /comandas/{id})
    └── dto/
        ├── CreateTableRequest.java
        ├── TableResponse.java
        ├── TableQrResponse.java                  ({presignedUrl, expiresAt})
        ├── ResolveTableResponse.java
        ├── OpenComandaRequest.java
        ├── JoinComandaRequest.java
        └── ComandaResponse.java

src/main/resources/db/migration/
└── V9__table_comanda_dinein.sql

src/main/java/com/cardapio/ordering/package-info.java   (modify: expose @NamedInterface("dineIn"))
src/main/resources/application.yml                       (modify: r2.* config + bucket name)
```

---

## Task 1: Domain VOs + enums extension

**Files:** `TableId`, `ComandaId`, `ComandaStatus`, `OrderModality` (modify), `OrderStatus` (modify)

- [ ] **Step 1.1:** `TableId` and `ComandaId` as record-VOs mirroring `OrderId`.
- [ ] **Step 1.2:** `ComandaStatus { OPEN, CLOSED }` with `isTerminal()`.
- [ ] **Step 1.3:** Add `DINE_IN` to `OrderModality`.
- [ ] **Step 1.4:** Add `SERVED` to `OrderStatus`. Add `ALLOWED_DINE_IN`:
  ```
  RECEIVED → CONFIRMED, CANCELED
  CONFIRMED → PREPARING, CANCELED
  PREPARING → SERVED
  SERVED → (terminal)
  ```
  Update `canTransitionTo` switch to include `DINE_IN -> ALLOWED_DINE_IN`. Update `isTerminal` to include `SERVED`.
- [ ] **Step 1.5:** Tests for the new transition table (allowed + denied transitions for DINE_IN).
- [ ] **Step 1.6:** Commit: `feat(ordering): extend modality and status for DINE_IN`

---

## Task 2: `Table` aggregate + tests

**Files:** `Table.java`, `TableTest.java`

- [ ] **Step 2.1:** Factory `Table.create(number, clock)` — generates `qrToken = UUID.randomUUID()`, `qrImageKey = null`, `active = true`. Number must be > 0 and unique (uniqueness enforced at repo level).
- [ ] **Step 2.2:** Methods `attachQrImageKey(String key)`, `deactivate()`, `activate()`. `qrToken` is **immutable** after creation.
- [ ] **Step 2.3:** Tests: factory invariants, qrToken not regenerated on subsequent ops.
- [ ] **Step 2.4:** Commit.

---

## Task 3: `Comanda` aggregate + tests

**Files:** `Comanda.java`, domain events, `ComandaTest.java`

- [ ] **Step 3.1:** Factory `Comanda.open(tableId, openerCustomerId, clock)` — status `OPEN`, `customerIds = [openerCustomerId]`, `orderIds = []`, `openedAt = now`. Records `ComandaOpened` event.
- [ ] **Step 3.2:** `join(customerId, clock)` — adds to `customerIds` (no-op if already present); throws `DineInInvariantException` if status is `CLOSED`. Records `ComandaJoined` if newly added.
- [ ] **Step 3.3:** `attachOrder(OrderId)` — appends; throws if `CLOSED`.
- [ ] **Step 3.4:** `close(clock)` — transitions to `CLOSED`, sets `closedAt`; idempotent if already closed (returns without re-emitting). Records `ComandaClosed`.
- [ ] **Step 3.5:** No business rule on closing requires "all orders paid" — this is a use-case-level guard (Task 8) so the aggregate stays cohesive.
- [ ] **Step 3.6:** Tests: open → join (idempotent) → attach order → close; close-while-closed is idempotent; join after close throws.
- [ ] **Step 3.7:** Commit.

---

## Task 4: `Order` invariants for DINE_IN

**Files:** `Order.java` (modify), tests

- [ ] **Step 4.1:** Add `tableId` and `comandaId` fields (both `Optional`/nullable).
- [ ] **Step 4.2:** Invariant in factory: if `modality == DINE_IN` then `tableId != null && comandaId != null`; if `modality != DINE_IN` then both must be null. Throws `DineInInvariantException`.
- [ ] **Step 4.3:** `deliveryAddress` must be null for `DINE_IN`; `deliveryFee = ZERO` (force) for `DINE_IN`.
- [ ] **Step 4.4:** Tests: each illegal combination rejected.
- [ ] **Step 4.5:** Commit.

---

## Task 5: JPA entities + Flyway V9 + repo adapters

**Files:** `TableJpaEntity`, `ComandaJpaEntity`, mappers, adapters, `V9__table_comanda_dinein.sql`

- [ ] **Step 5.1:** `tables` table — `id UUID PK`, `number INT UNIQUE NOT NULL`, `qr_token UUID UNIQUE NOT NULL`, `qr_image_key VARCHAR(200)`, `active BOOLEAN`, timestamps.
- [ ] **Step 5.2:** `comandas` table — `id UUID PK`, `table_id UUID NOT NULL` (no FK cross-schema, but soft index), `status VARCHAR(10)`, `opened_at TIMESTAMPTZ`, `closed_at TIMESTAMPTZ`, timestamps. Plus child tables `comanda_customers (comanda_id, customer_id)` and `comanda_orders (comanda_id, order_id)` (both PKs composite).
- [ ] **Step 5.3:** Index `idx_comandas_table_open ON comandas(table_id) WHERE status = 'OPEN'` — used by "currentComandaId" lookup.
- [ ] **Step 5.4:** Alter `orders` table — add `table_id UUID NULL`, `comanda_id UUID NULL`. (Existing rows are pre-DINE_IN, both null — OK.)
- [ ] **Step 5.5:** MapStruct mappers + adapters; `findOpenByTableId(tableId): Optional<Comanda>` is the key finder.
- [ ] **Step 5.6:** Adapter integration tests with Testcontainers.
- [ ] **Step 5.7:** Commit.

---

## Task 6: QR code generation + R2 storage adapter

**Files:** `QrCodeGenerator`, `QrStorage`, `ZxingQrCodeGenerator`, `R2Properties`, `R2Config`, `R2QrStorage`

- [ ] **Step 6.1:** `QrCodeGenerator.generatePng(String token, int sizePx): byte[]` — uses ZXing `QRCodeWriter`. Test against snapshot byte length / decodability (re-decode with ZXing reader).
- [ ] **Step 6.2:** `QrStorage` port — `putIfAbsent(String key, byte[] bytes, String contentType)` + `presignedUrl(String key, Duration ttl): URI`.
- [ ] **Step 6.3:** `R2Properties` — `accessKeyId`, `secretAccessKey`, `endpoint` (e.g. `https://<account>.r2.cloudflarestorage.com`), `bucket`, `region` (default `auto`).
- [ ] **Step 6.4:** `R2Config` builds `S3Client` and `S3Presigner` with custom endpoint and `PathStyleAccessEnabled(true)`.
- [ ] **Step 6.5:** `R2QrStorage` — putIfAbsent uses `headObject` then `putObject` to skip rewrite; `presignedUrl` uses `S3Presigner.presignGetObject` with TTL (default 10 min).
- [ ] **Step 6.6:** Wrap `R2QrStorage` calls with `@Retry(name="r2")` (5xx only; no circuit breaker — storage is critical, fail-fast is fine).
- [ ] **Step 6.7:** Tests: ZXing generator (decode roundtrip); R2 adapter via LocalStack-S3 Testcontainer (or skip integration and unit-test with mocked `S3Client`).
- [ ] **Step 6.8:** Commit.

---

## Task 7: Admin use cases — Create/List Table + Generate QR + Close Comanda

**Files:** `CreateTableUseCase`, `ListTablesUseCase`, `CloseComandaUseCase`, controllers `AdminTableController`, `AdminComandaController`

- [ ] **Step 7.1:** `CreateTableUseCase.execute(CreateTableCommand)`:
  1. Validate number unique.
  2. `Table.create(number, clock)` → save → generate PNG → `qrStorage.putIfAbsent("tables/" + tableId + ".png", bytes)` → `table.attachQrImageKey(key)` → save.
  3. Return `TableView` (no presigned URL yet — separate endpoint).
  Returns `Result<TableView>`.
- [ ] **Step 7.2:** `GET /admin/tables/{id}/qr` returns `{presignedUrl, expiresAt}` via `qrStorage.presignedUrl(table.qrImageKey, ttl=10min)`.
- [ ] **Step 7.3:** `ListTablesUseCase` returns all tables ordered by number, with derived `hasOpenComanda` flag (single query: `comandaRepository.findOpenByTableIdIn(...)`).
- [ ] **Step 7.4:** `CloseComandaUseCase.execute(CloseComandaCommand)`:
  1. Load comanda; assert `OPEN`.
  2. **Guard:** for each orderId in `comanda.orderIds`, fetch via `OrderRepository`; require all to be in a payable terminal-or-served state (i.e., `SERVED` or `CANCELED`). If not, return `Result.failure` with notification. (Decision: payment integration to comanda close is deferred — admin closes manually.)
  3. `comanda.close(clock)` → save. Domain events published.
  Returns `Result<ComandaSummaryView>`.
- [ ] **Step 7.5:** `AdminComandaController` — `GET /admin/comandas?status=OPEN`, `POST /admin/comandas/{id}/close`. Admin role guard via Spring Security.
- [ ] **Step 7.6:** Tests: use case unit tests + controller MockMvc.
- [ ] **Step 7.7:** Commit.

---

## Task 8: Customer use cases — Resolve / Open / Join

**Files:** `ResolveTableTokenUseCase`, `OpenComandaUseCase`, `JoinComandaUseCase`, controllers `TableResolveController`, `ComandaController`

- [ ] **Step 8.1:** `ResolveTableTokenUseCase.execute(token)`:
  - `tableRepository.findByQrToken(token)` → if absent or `!active` → `Result.failure("table.not-found")`.
  - Look up `comandaRepository.findOpenByTableId(tableId)` → maybe null.
  - Return `ResolveTableResponse(tableId, number, currentComandaId?)`.
  - **Public endpoint** (no JWT required) — front uses this to bootstrap the dine-in flow before login.
- [ ] **Step 8.2:** `OpenComandaUseCase.execute(OpenComandaCommand{tableId, customerId})`:
  - Idempotency: if there's already an open comanda for that table, return `Result.failure("comanda.already-open", existingId)` so client falls back to `join`. (Alt: auto-join — picked: explicit failure because client already has the resolve response.)
  - Validate customer exists via `IdentityFacade.getCustomerContact`.
  - Validate table active.
  - `Comanda.open(...)` → save → publish `ComandaOpened`.
- [ ] **Step 8.3:** `JoinComandaUseCase.execute(JoinComandaCommand{comandaId, customerId})`:
  - Load comanda; reject if `CLOSED`.
  - Validate customer exists.
  - `comanda.join(...)` → save (no-op if already joined; `Result.success` either way).
- [ ] **Step 8.4:** Controllers (customer JWT required for open/join; resolve is public). All endpoints under `/api/v1`.
- [ ] **Step 8.5:** Tests: unit + MockMvc; reject join on closed comanda; idempotent join.
- [ ] **Step 8.6:** Commit.

---

## Task 9: Place DINE_IN order — extend `PlaceOrderUseCase`

**Files:** `PlaceDineInOrderUseCase` (new) or extension of `PlaceOrderUseCase`, command, controller delta

- [ ] **Step 9.1:** Decision: **extend `PlaceOrderUseCase`** with optional `tableId`/`comandaId` in the command rather than duplicating. The use case already validates modality; we add a `DINE_IN` branch that requires both ids and fetches the comanda to attach the order id post-creation.
- [ ] **Step 9.2:** Validation:
  - If `modality = DINE_IN` and `tableId/comandaId` missing → `Result.failure("order.dine-in.missing-context")`.
  - Comanda must be `OPEN` and belong to the same `tableId`.
  - Customer must be a member of the comanda (i.e., must have called `join` first) — enforces the join step.
- [ ] **Step 9.3:** Force `deliveryAddress = null`, `deliveryFee = ZERO`.
- [ ] **Step 9.4:** After `Order` save, `comanda.attachOrder(orderId)` → save comanda.
- [ ] **Step 9.5:** Controller: existing `POST /orders` route handles all modalities — add the new fields to the request DTO; document via OpenAPI annotations.
- [ ] **Step 9.6:** Idempotency key path unchanged.
- [ ] **Step 9.7:** Tests: place DINE_IN order happy path; rejection if customer not joined; rejection if comanda closed.
- [ ] **Step 9.8:** Commit.

---

## Task 10: Order status transitions — `SERVED` flow

**Files:** existing admin status-advance controllers/use cases (modify), tests

- [ ] **Step 10.1:** The existing `AdvanceOrderStatusUseCase` already calls `OrderStatus.canTransitionTo(target, modality)` — with the enum changes from Task 1 it accepts the DINE_IN sequence automatically. Add a couple of integration tests to confirm.
- [ ] **Step 10.2:** Confirm `OrderStatusChanged` event fires on each transition (Phase 4 listener will already react — no change needed).
- [ ] **Step 10.3:** Commit.

---

## Task 11: NamedInterface + Modulith config

**Files:** `ordering/package-info.java` (modify), `ordering/application/dto/package-info.java` (modify or new)

- [ ] **Step 11.1:** Expose new DTO views under `@NamedInterface("dineIn")` (or merge into existing exposed `dto` namespace if one exists). Inspect Phase 4 plan for the exposed names and stay consistent.
- [ ] **Step 11.2:** Run `./mvnw test -Dtest=ModulithVerificationTest`.
- [ ] **Step 11.3:** Commit.

---

## Task 12: Configuration + `application.yml` + secrets

**Files:** `application.yml`, `application-dev.yml`, `application-test.yml`, `README.md`

- [ ] **Step 12.1:** Add:
```yaml
r2:
  access-key-id: ${R2_ACCESS_KEY_ID:}
  secret-access-key: ${R2_SECRET_ACCESS_KEY:}
  endpoint: ${R2_ENDPOINT:}
  bucket: ${R2_BUCKET:cardapio-qr}
  region: auto

qr:
  size-px: 512
  presigned-url-ttl-seconds: 600

resilience4j:
  retry:
    instances:
      r2: { max-attempts: 3, wait-duration: 500ms, exponential-backoff-multiplier: 2 }
```
- [ ] **Step 12.2:** `application-test.yml`: dummy keys; consider mocking `QrStorage` bean entirely in tests via `@TestConfiguration`.
- [ ] **Step 12.3:** Update README with new env vars: `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_ENDPOINT`, `R2_BUCKET`.
- [ ] **Step 12.4:** Commit.

---

## Task 13: Bruno collection — Mesa + Comanda flows

**Files:**
- `bruno/.../tables/admin-create-table.bru`
- `bruno/.../tables/admin-list-tables.bru`
- `bruno/.../tables/admin-table-qr.bru`
- `bruno/.../tables/customer-resolve-token.bru`
- `bruno/.../comandas/customer-open-comanda.bru`
- `bruno/.../comandas/customer-join-comanda.bru`
- `bruno/.../comandas/customer-place-dine-in-order.bru`
- `bruno/.../comandas/admin-close-comanda.bru`

- [ ] **Step 13.1:** Each request stores `tableId` / `comandaId` to env variables for chaining.
- [ ] **Step 13.2:** Commit.

---

## Task 14: Modulith verification + integration test

- [ ] **Step 14.1:** `./mvnw test -Dtest=ModulithVerificationTest`.
- [ ] **Step 14.2:** New `DineInFlowIntegrationTest`:
  1. Admin creates table → asserts QR object stored (mock storage in test).
  2. Customer A resolves token → opens comanda.
  3. Customer B resolves token → joins.
  4. Both place DINE_IN orders.
  5. Admin advances both `RECEIVED → CONFIRMED → PREPARING → SERVED`.
  6. Admin closes comanda → asserts `CLOSED`, `ComandaClosed` event published.
- [ ] **Step 14.3:** Commit.

---

## Task 15: Manual smoke

- [ ] **Step 15.1:** Run dev profile with real R2 credentials → create table → fetch presigned QR URL → scan with phone → open in browser; confirm token resolves.
- [ ] **Step 15.2:** Two browser sessions (two customers) open and join the same comanda; place orders; admin advances; admin closes. Confirm DB state.

---

## Done definition

- All checkboxes ticked.
- `./mvnw verify` green (unit + integration + Modulith verifier).
- `ordering` module exposes the new DTOs via `@NamedInterface` without breaking existing consumers (`payment`, `notification`).
- Manual smoke for the full DINE_IN flow passed.
- README updated with R2 env vars.

---

## Risks & open questions

| Item | Risk | Mitigation |
|---|---|---|
| QR token leak (someone screenshots a QR and orders remotely) | "Phantom orders" charged to wrong table | Token-only resolve doesn't open a comanda; opening requires authenticated customer JWT. Acceptable for MVP. |
| Two customers race to open the same comanda | Two `OPEN` comandas on same table | Add unique partial index `WHERE status='OPEN'` on `(table_id)` — DB rejects the second; second client falls back to `join`. |
| Closing a comanda before all orders are SERVED | Lost orders / billing mismatch | Use case guard requires all orders `SERVED` or `CANCELED`. |
| R2 bucket misconfigured (public ACL) | QR PNGs publicly listable | Use private bucket + presigned URLs only; document in README. |
| `Order` legacy rows have null `table_id`/`comanda_id` after migration | Mappers blow up | Both fields nullable in JPA entity + factory invariants only enforced when modality is DINE_IN. |
| Spring Modulith treats the new aggregates as crossing module boundary | Verifier fails | Aggregates live inside `ordering` module — no cross-module references; just new types in same package tree. |
