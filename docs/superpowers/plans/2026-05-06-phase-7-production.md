# Phase 7 — LGPD, Observability, Production Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the cardápio backend to production-ready quality. This phase is cross-cutting — it touches every module's logging, metrics, security boundary, and the deploy pipeline. Concrete deliverables:

1. **LGPD compliance** — `DELETE /api/v1/me` (anonimize + soft-delete the customer) and `GET /api/v1/me/data-export` (returns the customer's personal data + order history as JSON the user can download).
2. **Structured logging** — JSON access logs with request id (MDC) propagated across async boundaries; correlation id flows from controller → use case → adapter → notification listener.
3. **Metrics** — Micrometer registry feeding `/actuator/prometheus`. Custom counters/timers on the high-value flows (orders placed, payments, notification dispatch, reviews submitted).
4. **Rate limiting** — Bucket4j on the public auth + place-order paths to keep brute-force off the login + idempotency keys honest.
5. **Multi-replica scheduler safety** — ShedLock so the notification dispatcher (and any future schedulers) only fires on one node at a time, closing the Phase 4 risk.
6. **Production packaging** — Dockerfile multi-stage, `application-prod.yml`, secrets via env, health probes wired for orchestrators.

**Architecture:**
- LGPD delete is **soft**: customer row is kept (orders reference `customer_id`), but PII fields (`name`, `email`, `phone_number`, `password_hash`, social identities) are wiped/replaced with anonymous tombstones. New `deleted_at` timestamp on `customers`. Login routes refuse deleted accounts.
- Data export is JSON aggregating: customer profile (post-anonymization, before delete), all orders with items, all reviews, all comandas the customer joined. Cross-context — implemented as a use case in `identity` that calls `OrderingFacade` + `ReviewRepository` (read-only adapters).
- Logging: Logback layout switched to `LogstashEncoder` JSON in prod profile; MDC keys `requestId`, `customerId`, `correlationId` populated by a `OncePerRequestFilter`. `@ApplicationModuleListener` listeners wrap their handler in `MDC.put` so async events keep the requestId from the originator (read from event metadata).
- Metrics: Spring Boot already exposes JVM/HTTP via Actuator. We add the Prometheus registry, plus a small `Metrics` facade in `shared` so domain code does not import Micrometer directly.
- Rate limiting: a `BucketRateLimiterFilter` runs after CORS but before security on the targeted paths (`/auth/login`, `/auth/social/*`, `/orders` POST). Storage is in-memory (`Caffeine`) for MVP; Phase 7+ would migrate to Redis.
- ShedLock: `@SchedulerLock` on `OutboxDispatchScheduler.tick()` with a Postgres-backed lock provider (`shedlock-provider-jdbc-template`). Lock TTL = 5×fixedDelay (so a stuck node releases on death + JVM restart).
- Docker: multi-stage build (Maven → eclipse-temurin:21-jre), JLink-trimmed JRE deferred to optimization pass. Secrets only via env, no `.env` in image.

**Tech Stack additions:**
- `com.bucket4j:bucket4j-core:8.10.1` (single jar; we don't need the Spring starter)
- `com.bucket4j:bucket4j-caffeine:8.10.1` for the in-memory backend
- `com.github.ben-manes.caffeine:caffeine` (already a transitive dep of Spring; explicit if missing)
- `net.javacrumbs.shedlock:shedlock-spring:5.16.0`
- `net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.16.0`
- `net.logstash.logback:logstash-logback-encoder:8.0` for JSON logs
- `io.micrometer:micrometer-registry-prometheus` (Spring-managed version)

**Reference:**
- Spec: [docs/superpowers/specs/2026-05-04-cardapio-digital-backend-design.md](../specs/2026-05-04-cardapio-digital-backend-design.md) — §9 Phase 7
- Phase 4 plan: [docs/superpowers/plans/2026-05-06-phase-4-notifications.md](./2026-05-06-phase-4-notifications.md) — scheduler that needs ShedLock
- Phase 6c plan: [docs/superpowers/plans/2026-05-06-phase-6c-social-login.md](./2026-05-06-phase-6c-social-login.md) — Customer aggregate that LGPD delete now has to anonymize

**Out of scope (deferred):**
- Distributed rate limiting (Redis-backed Bucket4j) — single-replica-per-region MVP.
- Tracing (OpenTelemetry / Tempo / Zipkin) — only metrics + logs in this pass; trace propagation happens via MDC `correlationId` for the JSON log correlation.
- Alerting (Alertmanager rules, dashboards, PagerDuty) — out of scope; the plan delivers `/actuator/prometheus` so ops can wire it.
- Web Application Firewall / DDoS / TLS termination — assumed to be the responsibility of the reverse proxy / cloud LB.
- Audit log of admin actions — Phase 8+; the structured access log captures most of this for the MVP.
- Account merging (one customer linking two distinct emails) — punted from Phase 6c, still out.
- Cookie / CSRF tightening for the SSE endpoints — those endpoints already use bearer auth.

---

## File Structure

```
src/main/java/com/cardapio/
├── api/
│   └── error/
│       └── (existing — touched if rate-limit emits 429)
├── shared/
│   ├── observability/
│   │   ├── RequestContextFilter.java       (OncePerRequestFilter — sets MDC requestId/customerId)
│   │   ├── MdcContext.java                 (utility — keys + helpers)
│   │   └── ApplicationListenerMdcAspect.java (wraps @ApplicationModuleListener with MDC propagation)
│   └── metrics/
│       └── DomainMetrics.java              (Micrometer thin wrapper used by use cases)
├── api/
│   └── support/
│       └── ratelimit/
│           ├── RateLimitFilter.java
│           ├── RateLimitProperties.java
│           └── RateLimitConfig.java
├── identity/
│   ├── application/
│   │   ├── command/
│   │   │   └── DeleteMyAccountCommand.java
│   │   ├── usecase/
│   │   │   ├── DeleteMyAccountUseCase.java
│   │   │   └── ExportMyDataUseCase.java
│   │   └── dto/
│   │       └── CustomerDataExport.java     (full JSON shape: profile, orders, reviews, comandas)
│   ├── domain/
│   │   └── model/
│   │       └── Customer.java               (modify: anonymize() + deletedAt)
│   ├── infrastructure/
│   │   └── persistence/
│   │       └── jpa/
│   │           └── CustomerJpaEntity.java  (modify: deleted_at column)
│   ├── api/
│   │   └── rest/
│   │       └── MeController.java           (modify: DELETE + /me/data-export)
│   └── application/
│       └── IdentityFacade.java             (modify: deleteMyAccount, exportMyData)
├── ordering/application/
│   └── OrderingFacade.java                 (modify: listCustomerOrdersForExport)
└── review/application/
    └── (existing query reused — listMyReviews already returns enough)

src/main/resources/db/migration/
├── V13__customer_soft_delete.sql           (deleted_at column on customers)
└── V14__shedlock.sql                       (shedlock table)

src/main/resources/
├── application.yml                         (modify: prometheus exposed, rate-limit, shedlock)
├── application-prod.yml                    (NEW: JSON logs, prod connection pool, prod CORS)
├── logback-spring.xml                      (NEW: dev plain text + prod JSON via profile switch)

Dockerfile                                  (NEW: multi-stage, eclipse-temurin)
docker-compose.yml                          (modify if exists, else NEW: postgres + app for local prod-like)
.dockerignore                               (NEW)
README.md                                   (modify: env vars + run prod section)
```

---

## Task 1: Soft-delete column + Customer.anonymize()

**Files:** `Customer.java` (modify), `CustomerJpaEntity.java` (modify), `CustomerMapper.java` (modify), `V13__customer_soft_delete.sql`

- [ ] **Step 1.1:** Migration:
```sql
ALTER TABLE customers ADD COLUMN deleted_at TIMESTAMP(6) WITH TIME ZONE;
CREATE INDEX idx_customers_deleted_at ON customers (deleted_at) WHERE deleted_at IS NOT NULL;
```
- [ ] **Step 1.2:** `Customer.anonymize(Clock)` — replaces `name` with `"deleted-user-" + idShort`, replaces `email` with `"deleted+" + idShort + "@cardapio.local"` (still unique, recoverable to id), clears `phoneNumber`, clears `passwordHash`, clears `socialIdentities`. Sets `deletedAt = clock.instant()`. The aggregate's "at least one auth method" invariant is bypassed for deleted accounts — guard the constructor with a `deletedAt != null` short-circuit.
- [ ] **Step 1.3:** `Customer.isDeleted()` accessor.
- [ ] **Step 1.4:** `LoginCustomerUseCase` (and the social login use case) refuse a customer where `isDeleted()` — returns `INVALID_CREDENTIALS` (don't leak existence).
- [ ] **Step 1.5:** `CustomerJpaEntity` adds `deleted_at` column + getter/setter; mapper round-trips it.
- [ ] **Step 1.6:** Tests:
  - `anonymize` clears PII and sets `deletedAt`.
  - Constructor accepts an anonymized customer (no auth methods, but `deletedAt` is set).
  - Repeated calls to `anonymize` are idempotent.
- [ ] **Step 1.7:** Commit: `feat(identity): add soft-delete and anonymize on Customer`.

---

## Task 2: `DeleteMyAccountUseCase` + endpoint

**Files:** `DeleteMyAccountUseCase`, `DeleteMyAccountCommand`, `IdentityFacade` (modify), `MeController` (modify), `ErrorCode` (entry), tests

- [ ] **Step 2.1:** Add `ErrorCode.ACCOUNT_ALREADY_DELETED`.
- [ ] **Step 2.2:** `DeleteMyAccountCommand(CustomerId)` (subject from JWT).
- [ ] **Step 2.3:** Use case:
  1. Load customer → if absent or already `isDeleted` → `Result.failWith(...)`.
  2. `customer.anonymize(clock)`.
  3. `customers.save(customer)`.
  4. Revoke all refresh tokens for this customer (`refreshTokens.revokeAllForSubject(customerId)`).
  5. Return `Result.ok()`.
- [ ] **Step 2.4:** New finder/mutator on `RefreshTokenRepository`: `revokeAllForSubject(UUID subject)`. Implementation deletes (or marks revoked) all tokens for the customer.
- [ ] **Step 2.5:** `MeController` adds `DELETE /api/v1/me` returning `204`.
- [ ] **Step 2.6:** Tests: deletion path, double-delete returns typed error, refresh tokens are gone.
- [ ] **Step 2.7:** Commit.

---

## Task 3: `ExportMyDataUseCase` + endpoint

**Files:** `ExportMyDataUseCase`, `CustomerDataExport`, `MeController` (modify), `OrderingFacade` (modify with read method)

- [ ] **Step 3.1:** Add `OrderingFacade.listCustomerOrdersForExport(UUID customerId): List<OrderView>` (paged-by-customer, no upper bound for export — but cap at e.g. 1000 for safety; document in javadoc).
- [ ] **Step 3.2:** `CustomerDataExport` record:
  ```java
  record CustomerDataExport(
      UUID id, String name, String email, String phoneNumber,
      List<SocialIdentitySummary> socialIdentities,
      List<OrderSummary> orders,
      List<ReviewSummary> reviews,
      List<ComandaSummary> comandas,
      Instant exportedAt
  ) {}
  ```
- [ ] **Step 3.3:** Use case calls:
  - `customers.findById(customerId)` → 404 if missing.
  - `OrderingFacade.listCustomerOrdersForExport(customerId)`.
  - `ReviewRepository.findAllByCustomer(customerId)` (new finder on the existing repo — read-only).
  - `ComandaRepository.findAllByMember(customerId)` (new finder, queries the join table).
- [ ] **Step 3.4:** `MeController` adds `GET /api/v1/me/data-export` returning the JSON with `Content-Disposition: attachment; filename="cardapio-export-<id>.json"` so browsers offer download.
- [ ] **Step 3.5:** Use case is `@Transactional(readOnly=true)`.
- [ ] **Step 3.6:** Tests: structure assertions; redact `passwordHash` (never serialized); ensure the export doesn't include other customers' data (use case scopes by customerId at every step).
- [ ] **Step 3.7:** Commit.

---

## Task 4: Structured JSON logging + MDC

**Files:** `RequestContextFilter`, `MdcContext`, `logback-spring.xml`, `pom.xml` (logstash-logback-encoder), `application-prod.yml`

- [ ] **Step 4.1:** `MdcContext` constants: `MDC_REQUEST_ID = "requestId"`, `MDC_CUSTOMER_ID = "customerId"`, `MDC_CORRELATION_ID = "correlationId"`. Helpers `runWith(Map<String,String>, Runnable)` for async use.
- [ ] **Step 4.2:** `RequestContextFilter extends OncePerRequestFilter`:
  - Read `X-Request-Id` header if present, else generate a UUID.
  - `MDC.put(MDC_REQUEST_ID, ...)`. After `chain.doFilter(...)`, `MDC.clear()` in `finally`.
  - Echo `X-Request-Id` back on the response so the front-end can correlate.
  - On authenticated requests (after security filter ran), pull customerId from `SecurityContextHolder` and put it in MDC. Pick: do this in a separate filter ordered after the JWT filter, OR pull lazily in a filter (chosen: separate filter — keeps responsibilities clean).
- [ ] **Step 4.3:** `logback-spring.xml`:
  - `<springProfile name="dev,test">` — pattern with `[%mdc{requestId}] ...` for human readability.
  - `<springProfile name="prod">` — `LogstashEncoder` for JSON, including `customCustomFields` for app name and `mdc-include` for requestId, customerId, correlationId.
- [ ] **Step 4.4:** Async listeners (Phase 4 notification, Phase 6a coupon, Phase 6b review): the Modulith outbox already wraps the async dispatch and the `ApplicationModuleListener` runs in a separate task; MDC won't propagate by default. Add `TaskDecorator` bean copying MDC into the task threadpool (Spring's `@Async` uses an `Executor`; we configure the framework's default with the decorator).
- [ ] **Step 4.5:** Tests: `MockMvc` request with `X-Request-Id: foo` → response carries it back; with no header → response has a UUID. (logback output assertions are brittle — skip.)
- [ ] **Step 4.6:** Commit.

---

## Task 5: Metrics + Prometheus

**Files:** `pom.xml` (Prometheus registry), `application.yml`, `DomainMetrics.java`, modify chosen use cases to emit counters

- [ ] **Step 5.1:** Add `io.micrometer:micrometer-registry-prometheus` (Spring-managed version).
- [ ] **Step 5.2:** `application.yml` — expose actuator endpoints `health, info, metrics, prometheus`.
- [ ] **Step 5.3:** `DomainMetrics`:
  ```java
  Counter ordersPlaced(OrderModality modality);
  Counter paymentsApproved();
  Counter notificationsDispatched(NotificationChannel channel, NotificationStatus status);
  Counter reviewsSubmitted();
  Timer  paymentGatewayLatency();
  ```
  - Inject `MeterRegistry`. Cache counters by tag in a `Map<TagKey, Counter>` to avoid per-call lookups.
- [ ] **Step 5.4:** Wire into:
  - `PlaceOrderUseCase` — increment `ordersPlaced(modality)` after success.
  - `PaymentApprovedListener` (existing in ordering) — increment `paymentsApproved`.
  - `DispatchOutboxUseCase` — increment `notificationsDispatched(channel, SENT|FAILED)`.
  - `SubmitReviewUseCase` — increment `reviewsSubmitted`.
  - `MercadoPagoGateway` adapter — `Timer.record(...)` around the HTTP call.
- [ ] **Step 5.5:** Test: a small unit test using `SimpleMeterRegistry` to assert counters increment on the relevant path.
- [ ] **Step 5.6:** Commit.

---

## Task 6: Rate limiting (Bucket4j)

**Files:** `RateLimitFilter`, `RateLimitProperties`, `RateLimitConfig`, `pom.xml` (bucket4j)

- [ ] **Step 6.1:** `RateLimitProperties`:
```yaml
rate-limit:
  enabled: true
  key: ip                     # ip | user
  rules:
    - paths: [/api/v1/auth/login, /api/v1/admin/auth/login]
      capacity: 10
      refill-tokens: 10
      refill-period: 1m
    - paths: [/api/v1/auth/social/google, /api/v1/auth/social/apple]
      capacity: 30
      refill-tokens: 30
      refill-period: 1m
    - paths: [/api/v1/orders]
      methods: [POST]
      capacity: 60
      refill-tokens: 60
      refill-period: 1m
```
- [ ] **Step 6.2:** `RateLimitConfig` builds one Bucket4j proxy per rule, backed by a `Caffeine` cache keyed by `(ruleId, clientKey)` with eviction window matching the largest refill period × 2.
- [ ] **Step 6.3:** `RateLimitFilter extends OncePerRequestFilter`:
  - Match request path + method against rules.
  - Resolve `clientKey`: when the rule key is `ip`, use `X-Forwarded-For` first chunk (fallback to `request.getRemoteAddr()`); when `user`, pull subject from auth (skip rule if anonymous).
  - `bucket.tryConsume(1)` → if false, write `429 Too Many Requests` with `Retry-After` header equal to ceil(refill period in seconds for one token).
  - Filter ordered before the JWT filter for IP-keyed rules; after for user-keyed rules. Pick: ordered before — IP-key is enough for MVP.
- [ ] **Step 6.4:** `application.yml` — add `rate-limit.enabled: true` (default off in `application-test.yml`).
- [ ] **Step 6.5:** Tests: hammer `/auth/login` with N+1 requests from same IP → assert N succeed and the (N+1)-th gets 429. Use `MockMvc`.
- [ ] **Step 6.6:** Commit.

---

## Task 7: ShedLock for the notification scheduler

**Files:** `pom.xml` (shedlock), `OutboxDispatchScheduler.java` (modify), `V14__shedlock.sql`, `ShedLockConfig.java`

- [ ] **Step 7.1:** Migration:
```sql
CREATE TABLE shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    locked_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
```
- [ ] **Step 7.2:** `ShedLockConfig` registers a `JdbcTemplateLockProvider` bean and `@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")`.
- [ ] **Step 7.3:** Annotate `OutboxDispatchScheduler.tick()` with `@SchedulerLock(name = "notification.outbox", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")`.
- [ ] **Step 7.4:** Tests: existing notification dispatcher IT still passes (we just add the annotation; lock behaviour requires multi-instance to verify and is out of scope for unit tests).
- [ ] **Step 7.5:** Commit.

---

## Task 8: Production packaging — Dockerfile + application-prod.yml + healthchecks

**Files:** `Dockerfile`, `.dockerignore`, `docker-compose.prod.yml` (or extend existing), `application-prod.yml`, README

- [ ] **Step 8.1:** Multi-stage `Dockerfile`:
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -e -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
USER 1001
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
```
- [ ] **Step 8.2:** `.dockerignore`: target, .git, .idea, .mvn/wrapper, *.iml, .env*.
- [ ] **Step 8.3:** `application-prod.yml`:
  - `spring.datasource.hikari.{maximum-pool-size: 20, minimum-idle: 5}`
  - `spring.jpa.show-sql: false`
  - `management.endpoints.web.exposure.include: health,info,metrics,prometheus`
  - `management.endpoint.health.probes.enabled: true` (Kubernetes-style liveness/readiness)
  - `management.endpoint.health.show-details: never`
  - `notification.dispatch.fixed-delay-ms: 30000` (same as dev — leave room for ops).
  - `oauth.google.enabled: ${OAUTH_GOOGLE_ENABLED:false}` (carries from base — no override).
- [ ] **Step 8.4:** README: add "Run locally", "Run in Docker", "Required env vars" table consolidating every secret introduced across phases.
- [ ] **Step 8.5:** Smoke: `docker build .` succeeds; container starts; `/actuator/health` returns 200 (manual step).
- [ ] **Step 8.6:** Commit.

---

## Task 9: Bruno collection — LGPD endpoints

**Files:** `bruno/18_LGPD/*.bru`

- [ ] **Step 9.1:** Customer Data Export — `GET /me/data-export`.
- [ ] **Step 9.2:** Customer Delete Account — `DELETE /me`.
- [ ] **Step 9.3:** Customer Login After Delete (error) — same credentials → 401.
- [ ] **Step 9.4:** Commit.

---

## Task 10: Modulith verification + integration tests

- [ ] **Step 10.1:** `./mvnw test -Dtest=ModulithVerificationTest`.
- [ ] **Step 10.2:** New `LgpdFlowIT`:
  1. Register customer + place an order + submit a review.
  2. `GET /me/data-export` → 200 with the customer + the order + the review embedded.
  3. `DELETE /me` → 204.
  4. `GET /me/data-export` again → still works (returns anonymized profile + the historical orders/reviews — those records survive deletion).
  5. `POST /auth/login` with original credentials → 401.
- [ ] **Step 10.3:** New `RateLimitIT`: Hammer `/auth/login` with 11 requests in 1 minute from the same IP → 11th returns 429.
- [ ] **Step 10.4:** Commit.

---

## Task 11: Manual smoke

- [ ] **Step 11.1:** `docker compose up` (or `mvn spring-boot:run` with `prod` profile) and verify `/actuator/prometheus` returns metrics; `cardapio_orders_placed_total` shows up after one Bruno place-order request.
- [ ] **Step 11.2:** Tail JSON logs and confirm `requestId` present + propagated across an order placement (controller → use case → notification listener).
- [ ] **Step 11.3:** Stress-test login with `wrk` or `ab` until 429s start coming back.
- [ ] **Step 11.4:** Run the LGPD flow on a clean account end-to-end through Bruno.

---

## Done definition

- All checkboxes ticked.
- `./mvnw verify` green (unit + integration + Modulith verifier).
- `docker build .` produces a runnable image; container responds to `/actuator/health`.
- LGPD: a customer can export then delete their account; subsequent login is denied; historical orders survive (referential integrity preserved).
- Spec coverage: §9 Phase 7 in full.

---

## Risks & open questions

| Item | Risk | Mitigation |
|---|---|---|
| Soft-delete leaks email by anonymizing into a predictable pattern | Attacker enumerates `deleted+<idShort>@cardapio.local` to confirm an account existed | Anonymized email is unique-per-customer-id; observing it requires admin DB access. The unique-email constraint is what forces the substitution. |
| Data export contains PII of other customers (e.g. comanda members) | Privacy violation | Comanda summary in export lists only the requester's own membership flag, not the other members' UUIDs. |
| Logstash JSON encoder dropped in dev | Hard to read logs | logback-spring profile switch keeps dev plain text. |
| `@Async` thread pool drops MDC | Lost requestId in async listeners | `TaskDecorator` copies MDC into the executor's thread. |
| Bucket4j blocks legitimate burst traffic (e.g. CI smoke tests) | False 429s | Test profile sets `rate-limit.enabled: false`. Rules are tuned generous (60 orders/min/IP). |
| ShedLock holds the lock past the next tick if the JVM dies in the middle | Backlog grows | `lockAtMostFor=PT5M` lets a stuck lock auto-release. The dispatcher itself is idempotent on per-row basis (Phase 4 outbox semantics) so retry is safe. |
| Docker image too large (~250MB with full JRE) | Slow deploys | Acceptable for MVP; Phase 7+ optimization considers JLink. |
| `/actuator/prometheus` exposed without auth | Metric scraping by anyone | Spec says actuator endpoints behind auth in prod; we add `management.endpoints.web.base-path=/actuator` and tighten via SecurityConfig if not already. Document in README that prod deploys should restrict actuator via reverse proxy. |
| Existing customers refreshed through MeController might break if `phoneNumber()` is empty | New `data-export` exposes nullable | Export DTO uses `Optional`-friendly nullable strings; tests assert null is preserved. |
| LGPD compliance team wants audit trail of deletes | "Who deleted, when?" | `customers.deleted_at` plus the access log entry from `RequestContextFilter` (the JSON log carries customerId + requestId). Sufficient for MVP. |
