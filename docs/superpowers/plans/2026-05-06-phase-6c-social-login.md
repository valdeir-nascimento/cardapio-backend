# Phase 6c — Social Login (Google + Apple) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let customers sign in (and sign up on first contact) via Google or Apple instead of email/password. The front-end obtains a provider ID token (Google Sign-In JS or Apple's `AppleID.auth.signIn`) and POSTs it to the backend. The backend verifies the JWT against the provider's published JWKS, then either:

1. **Returns tokens for an existing customer** when the provider `subject` is already linked, or when the verified `email` matches an existing email-registered customer — in which case the social identity is **linked** to that customer.
2. **Creates a new customer** with no password and the social identity attached.

In both cases the response is the same `TokenPair` shape as `/auth/login`.

**Architecture:**
- `Customer` aggregate gains `socialIdentities: List<SocialIdentity>` and makes `passwordHash` + `phoneNumber` optional. A customer must have **at least one** auth path: a `passwordHash` OR a `SocialIdentity`. (The aggregate enforces this; the DB allows nulls.)
- New entity inside the aggregate: `SocialIdentity { provider, subject, emailAtLink, linkedAt }`. Unique by `(provider, subject)` at the DB level.
- New port `IdTokenVerifier` per provider — two implementations (`GoogleIdTokenVerifier`, `AppleIdTokenVerifier`) using a shared JWKS fetcher (`JwksClient` with TTL cache).
- New use case `LoginWithSocialIdentityUseCase` orchestrating verify → link-or-create → issue tokens.
- REST: `POST /api/v1/auth/social/google` and `POST /api/v1/auth/social/apple`. Public (no JWT required).

**Cross-context contracts:** none new. `notification`'s existing `CustomerContact` lookup keeps working because `email` is still the primary identifier.

**Tech Stack additions:**
- `com.auth0:java-jwt:4.4.0` for JWT verification with RSA keys (Google uses RS256, Apple uses ES256). Lighter than auth0/jwks-rsa and avoids a transitive Guava version conflict with our SDKs. We'll write the JWKS fetcher ourselves (~80 lines, plain `RestClient`).
- No new SDKs from Google or Apple. Both providers expose the OIDC discovery + JWKS endpoints over HTTPS.

**Reference:**
- Spec: [docs/superpowers/specs/2026-05-04-cardapio-digital-backend-design.md](../specs/2026-05-04-cardapio-digital-backend-design.md) — §2 contexts (`identity` carries `SocialIdentity[]`), §3 identity, §9 Phase 6 (login social Google + Apple)
- Existing `AbstractLoginUseCase`: [src/main/java/com/cardapio/identity/application/usecase/AbstractLoginUseCase.java](../../../src/main/java/com/cardapio/identity/application/usecase/AbstractLoginUseCase.java) — token issuance pattern to reuse

**Out of scope (deferred):**
- Multiple saved addresses on `Customer` (spec §9 Phase 6 line "Múltiplos endereços (se faltou na Fase 1)") — punted to Phase 7. Existing flows accept address per-order via `PlaceOrderRequest`, which is enough for MVP delivery.
- Account merging UI (existing customer chooses to link a different existing email) — current MVP just refuses if subject is taken or email belongs to someone else's account.
- Two-factor for password customers — Phase 7+.
- Anonymous reset / password recovery — Phase 7.
- Apple's email-relay reuse across re-signins (Apple emails are stable per Apple ID + app, so we use them as-is).
- Customer-initiated unlink of a social provider — admin can do it manually if needed.

---

## File Structure

```
src/main/java/com/cardapio/identity/
├── domain/
│   ├── model/
│   │   ├── SocialProvider.java                    (enum: GOOGLE, APPLE)
│   │   ├── SocialIdentity.java                    (entity inside Customer)
│   │   └── Customer.java                          (modify: passwordHash/phoneNumber optional, socialIdentities list)
│   ├── port/
│   │   └── IdTokenVerifier.java                   (interface — per-provider impl)
│   └── exception/
│       ├── CustomerWithoutAuthMethodException.java
│       └── SocialIdentityAlreadyLinkedException.java
├── application/
│   ├── command/
│   │   └── LoginWithSocialIdentityCommand.java    (provider, idToken)
│   ├── usecase/
│   │   └── LoginWithSocialIdentityUseCase.java
│   └── IdentityFacade.java + Impl                 (modify: add loginWithSocial)
├── infrastructure/
│   ├── persistence/
│   │   ├── jpa/
│   │   │   ├── CustomerJpaEntity.java             (modify: nullable cols, child collection)
│   │   │   └── SocialIdentityJpaEntity.java       (NEW; or @ElementCollection — pick @ElementCollection)
│   │   ├── repository/
│   │   │   └── SpringCustomerJpaRepository.java   (modify: findBySocialIdentity)
│   │   └── mapper/
│   │       └── CustomerMapper.java                (modify)
│   └── security/
│       ├── jwks/
│       │   ├── JwksClient.java                    (HTTP cache around /jwks endpoint)
│       │   ├── JwksProperties.java                (cache TTL)
│       │   └── CachedJwksProvider.java
│       └── idtoken/
│           ├── GoogleIdTokenVerifier.java
│           └── AppleIdTokenVerifier.java
├── api/
│   ├── rest/
│   │   └── SocialAuthController.java
│   └── dto/
│       └── SocialLoginRequest.java                (idToken)

src/main/resources/db/migration/
└── V12__customer_social_identities.sql

src/main/resources/application.yml                 (modify: oauth.google.client-id, oauth.apple.client-id, jwks cache ttl)
```

---

## Task 1: Domain refactor — `SocialIdentity` + optional auth methods on `Customer`

**Files:** `SocialProvider`, `SocialIdentity`, `Customer` (modify), `CustomerWithoutAuthMethodException`, tests

- [ ] **Step 1.1:** `SocialProvider { GOOGLE, APPLE }`.
- [ ] **Step 1.2:** `SocialIdentity` immutable record-style class (`provider`, `subject`, `emailAtLink`, `linkedAt`). `subject` is the provider's stable user id (e.g. Google `sub` claim). `emailAtLink` is captured for audit only — we don't trust it for routing later.
- [ ] **Step 1.3:** `Customer.register(...)` overloads:
  - Existing email/password registration keeps working (passwordHash + phoneNumber required).
  - New `Customer.registerSocial(name, email, SocialIdentity, clock)` creates a customer with no password, no phone, and the social identity attached. `phoneNumber` remains `null` until the customer completes profile.
- [ ] **Step 1.4:** `linkSocialIdentity(SocialIdentity, clock)` — adds to the list; throws `SocialIdentityAlreadyLinkedException` if `(provider, subject)` already present on this aggregate (idempotent for same identity from same login retry — return without throwing if exact match).
- [ ] **Step 1.5:** `passwordHash()` returns `Optional<HashedPassword>`; `phoneNumber()` returns `Optional<PhoneNumber>`. Mutators that set them stay.
- [ ] **Step 1.6:** Invariant: `Customer` must always have at least one auth path → `passwordHash != null || !socialIdentities.isEmpty()`. Enforced in constructor + `unlinkSocialIdentity` (deferred — admin-only, not in this phase). Throws `CustomerWithoutAuthMethodException`.
- [ ] **Step 1.7:** `findSocialIdentity(SocialProvider)` accessor for the use case.
- [ ] **Step 1.8:** Tests:
  - `register` happy path unchanged.
  - `registerSocial` produces customer with empty password / phone but valid auth.
  - `linkSocialIdentity` rejects duplicate `(provider, subject)`; idempotent on exact match.
  - Constructor rejects a customer with neither password nor social.
- [ ] **Step 1.9:** Existing `LoginCustomerUseCase` and password change flow assume `passwordHash` is present — update them to `Result.failWith(INVALID_CREDENTIALS)` when a customer has no password (i.e. social-only).
- [ ] **Step 1.10:** Commit: `refactor(identity): allow social identities and optional password on Customer`.

---

## Task 2: JPA + Flyway V12 — relax columns + new collection

**Files:** `CustomerJpaEntity` (modify), `CustomerMapper` (modify), `V12__customer_social_identities.sql`, IT

- [ ] **Step 2.1:** Migration:
```sql
ALTER TABLE customers ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE customers ALTER COLUMN phone_number  DROP NOT NULL;

CREATE TABLE customer_social_identities (
    customer_id    UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    provider       VARCHAR(20) NOT NULL,
    subject        VARCHAR(200) NOT NULL,
    email_at_link  VARCHAR(180) NOT NULL,
    linked_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PRIMARY KEY (customer_id, provider),
    CONSTRAINT uk_social_identity_provider_subject UNIQUE (provider, subject)
);
```
- [ ] **Step 2.2:** `CustomerJpaEntity` annotates `passwordHash`/`phoneNumber` columns nullable. Adds `@ElementCollection` of `SocialIdentityEmbeddable` keyed on `(provider)` per customer, with `@CollectionTable(name="customer_social_identities", joinColumns=@JoinColumn(name="customer_id"))`. Mirrors the Phase 5 `Comanda.customer_ids` pattern.
- [ ] **Step 2.3:** `CustomerMapper` round-trips the new shape.
- [ ] **Step 2.4:** Persistence IT:
  - Save email/password customer → reload → `socialIdentities` empty, `password_hash` present.
  - Save social-only customer → reload → no password, one identity.
  - Two customers can't share `(provider, subject)` (unique constraint).
- [ ] **Step 2.5:** Spring Data finder: `findBySocialIdentitiesProviderAndSocialIdentitiesSubject(...)` — works with `@ElementCollection`. If the derived query proves flaky, write a JPQL `@Query` joining the collection table.
- [ ] **Step 2.6:** Commit.

---

## Task 3: JWKS cache + per-provider ID token verifiers

**Files:** `IdTokenVerifier` (port), `JwksClient`, `CachedJwksProvider`, `JwksProperties`, `GoogleIdTokenVerifier`, `AppleIdTokenVerifier`, tests with `MockRestServiceServer`

- [ ] **Step 3.1:** Add dependency `com.auth0:java-jwt:4.4.0` to `pom.xml`. (Avoid `auth0/jwks-rsa` to keep the stack small — we cache keys ourselves.)
- [ ] **Step 3.2:** `IdTokenVerifier` port:
  ```java
  public interface IdTokenVerifier {
      SocialProvider provider();
      VerifiedIdToken verify(String idToken) throws InvalidIdTokenException;
      record VerifiedIdToken(String subject, String email, String name) {}
  }
  ```
  - `subject` is the `sub` claim — never null.
  - `email` from the `email` claim (Google: present and verified; Apple: present on first login only — we capture and persist whatever we get).
  - `name` from `name`/`given_name`+`family_name` (Google) — Apple does not return name in the JWT (it's posted separately by the JS SDK on first login). The customer flow defaults to `email-local-part` if name is absent.
- [ ] **Step 3.3:** `JwksClient` fetches a JWK Set from a URL with a timeout. `CachedJwksProvider` wraps it with a 1-hour TTL (configurable). Cache invalidation: on `kid` miss, refetch once before failing — provider rotates keys.
- [ ] **Step 3.4:** `GoogleIdTokenVerifier`:
  - JWKS URL: `https://www.googleapis.com/oauth2/v3/certs`.
  - Issuer must be `https://accounts.google.com` or `accounts.google.com`.
  - Audience must equal configured `oauth.google.client-id`.
  - Algorithm: RS256.
  - Reject if `exp` < now or `iat` > now + 5 minutes (clock skew).
- [ ] **Step 3.5:** `AppleIdTokenVerifier`:
  - JWKS URL: `https://appleid.apple.com/auth/keys`.
  - Issuer must be `https://appleid.apple.com`.
  - Audience must equal configured `oauth.apple.client-id` (the Service ID, not the App ID).
  - Algorithm: ES256.
  - Same exp/iat checks as Google.
- [ ] **Step 3.6:** `InvalidIdTokenException` carries a typed reason (`EXPIRED`, `INVALID_SIGNATURE`, `INVALID_ISSUER`, `INVALID_AUDIENCE`, `MALFORMED`) so the controller can map it.
- [ ] **Step 3.7:** Tests using `MockRestServiceServer` to stub JWKS, plus a hand-signed RS256 token signed with a test keypair. One per provider, plus one error case per failure mode.
- [ ] **Step 3.8:** Commit.

---

## Task 4: `LoginWithSocialIdentityUseCase`

**Files:** command, use case, tests

- [ ] **Step 4.1:** Add `ErrorCode` entries: `INVALID_ID_TOKEN`, `SOCIAL_LOGIN_PROVIDER_UNAVAILABLE`, `EMAIL_BELONGS_TO_OTHER_CUSTOMER`.
- [ ] **Step 4.2:** Command `LoginWithSocialIdentityCommand(SocialProvider provider, String idToken)`.
- [ ] **Step 4.3:** Use case orchestration:
  1. Pick the verifier for `provider` from the injected `Map<SocialProvider, IdTokenVerifier>`.
  2. `verifier.verify(idToken)` → if it throws, return `Result.failWith(INVALID_ID_TOKEN, reason)`.
  3. Look up customer by `(provider, subject)` → if found, advance to step 6.
  4. Look up customer by `email` → if found:
     - Link the new `SocialIdentity` and save. (The email already proves identity since the provider verified it.)
  5. Else: register new customer via `Customer.registerSocial(name, email, identity, clock)`.
  6. Issue access + refresh tokens (mirror `AbstractLoginUseCase` token-issuing path; extract a small helper if duplication grows).
  7. Return `TokenPair`.
- [ ] **Step 4.4:** Apple-specific guard: if `email` is missing from the token (rare — Apple suppresses email after first sign-in if user revoked sharing), step 4 falls through to step 3 (subject lookup) and the use case fails if the subject is unknown. Return a typed error so the front-end can prompt re-share.
- [ ] **Step 4.5:** Tests:
  - First login → new customer created.
  - Second login with same subject → same customer, same id.
  - Email-known + first-time subject → linked.
  - Email-known but already has different SocialIdentity for same provider → reject (would violate `(customer_id, provider)` PK on the collection table; surface as `SOCIAL_IDENTITY_ALREADY_LINKED`).
  - Verifier throws → typed error.
- [ ] **Step 4.6:** Commit.

---

## Task 5: REST + Security wiring

**Files:** `SocialAuthController`, `SocialLoginRequest`, `SecurityConfig` (modify), `IdentityFacade` (modify)

- [ ] **Step 5.1:** `SocialLoginRequest(@NotBlank String idToken)`.
- [ ] **Step 5.2:** `SocialAuthController`:
  - `POST /api/v1/auth/social/google` → `LoginWithSocialIdentityUseCase` with `provider=GOOGLE`.
  - `POST /api/v1/auth/social/apple` → same with `APPLE`.
  - Returns `TokenPairResponse` (existing shape).
- [ ] **Step 5.3:** `SecurityConfig`: add the two paths to `permitAll` alongside existing `/auth/register`/`/auth/login`.
- [ ] **Step 5.4:** Add `loginWithSocial(...)` to `IdentityFacade` — exposes the new use case at the same level as `loginCustomer`.
- [ ] **Step 5.5:** MockMvc tests: 200 with mocked verifier; 401 on invalid token; 200 on link path.
- [ ] **Step 5.6:** Commit.

---

## Task 6: Configuration + secrets

**Files:** `application.yml`, `application-dev.yml`, `application-test.yml`, README

- [ ] **Step 6.1:** Add:
```yaml
oauth:
  google:
    enabled: ${OAUTH_GOOGLE_ENABLED:false}
    client-id: ${OAUTH_GOOGLE_CLIENT_ID:dummy.apps.googleusercontent.com}
    issuer: https://accounts.google.com
    jwks-uri: https://www.googleapis.com/oauth2/v3/certs
  apple:
    enabled: ${OAUTH_APPLE_ENABLED:false}
    client-id: ${OAUTH_APPLE_CLIENT_ID:com.example.dummy}
    issuer: https://appleid.apple.com
    jwks-uri: https://appleid.apple.com/auth/keys

jwks:
  cache-ttl: 1h
  fetch-timeout-ms: 5000
```
- [ ] **Step 6.2:** Verifier beans guarded by `@ConditionalOnProperty(prefix="oauth.<provider>", name="enabled", havingValue="true")`. The use case injects `Map<SocialProvider, IdTokenVerifier>` — entries are missing when a provider is disabled, so the use case returns `SOCIAL_LOGIN_PROVIDER_UNAVAILABLE` instead of crashing.
- [ ] **Step 6.3:** README env-var section adds `OAUTH_GOOGLE_CLIENT_ID`, `OAUTH_APPLE_CLIENT_ID`.
- [ ] **Step 6.4:** Commit.

---

## Task 7: Bruno collection — social auth

**Files:** `bruno/17_Social Auth/*.bru`

- [ ] **Step 7.1:** Two requests for `Login (Google)` and `Login (Apple)` with `idToken` header expected to be filled in by the operator (running this against a real Google flow needs a real token).
- [ ] **Step 7.2:** Error case: invalid token → 401.
- [ ] **Step 7.3:** Commit.

---

## Task 8: Modulith verification + flow IT

- [ ] **Step 8.1:** `./mvnw test -Dtest=ModulithVerificationTest`.
- [ ] **Step 8.2:** `SocialLoginFlowIT` with a stubbed `IdTokenVerifier` bean (Spring `@Primary` test bean) so we don't have to mint real Google/Apple tokens:
  1. Stubbed verifier returns `(subject="g-1", email="bob@example.com", name="Bob")`.
  2. POST `/api/v1/auth/social/google` → expect 200 + `TokenPairResponse`.
  3. Hit again → same flow, same customer (verify customer count didn't increase).
  4. POST with stubbed `(subject="g-2", email="bob@example.com")` → links into existing Bob, customer count still 1.
  5. POST with email already used by an email/password customer → linked.
- [ ] **Step 8.3:** Commit.

---

## Task 9: Manual smoke

- [ ] **Step 9.1:** `mvn spring-boot:run` with real Google client id. Use the Google Sign-In playground to mint an id_token and POST it via Bruno.
- [ ] **Step 9.2:** Verify a customer row was created in `customers` (no password) and a row in `customer_social_identities`.
- [ ] **Step 9.3:** Re-login with the same google account → same customer id, no new row.

---

## Done definition

- All checkboxes ticked.
- `./mvnw verify` green.
- Modulith verifier passes.
- Bruno flows exercise the social paths end-to-end on a running instance with at least one provider configured.
- Spec coverage: §3 identity (`SocialIdentity[]`), §9 Phase 6 (login Google + Apple).

---

## Risks & open questions

| Item | Risk | Mitigation |
|---|---|---|
| JWKS fetch fails (network blip) | Login outage | 1-hour cache + lazy refresh on `kid` miss. Errors surfaced as 503 with typed code; front-end can retry. |
| Provider rotates keys | All in-flight tokens fail | Cache miss falls back to a one-shot refetch before failing. |
| Apple suppresses email after first login | Login fails for a returning user | Use case looks up by `(provider, subject)` first — no email needed for returning users. |
| Customer signs in with social on an existing email/password account | Possible account takeover if attacker controls a Google account with the victim's email | Provider has already verified the email. We treat `email_verified=true` as the link signal. Reject Google tokens where `email_verified=false`. (Apple emails are inherently verified.) |
| Two customers fight over the same email (one with password, one trying social) | Existing email customer suddenly gets a stranger's social identity linked | Same as above — provider verifies email. Customer is the same person, no takeover. The "same person on two devices with different google accounts" case is rejected by `(provider, subject)` UNIQUE. |
| Existing tests assume `passwordHash` non-null | Compile breakage | Audit at Task 1 step 1.9 — check `LoginCustomerUseCase`, `RegisterCustomerUseCase`, `UpdateMyProfileUseCase`, mappers. |
| ArchUnit catches infrastructure imports leaking into application | Phase 6b déjà-vu | Verifier checks at Task 8. Use ports for verifiers. |
| Apple JWT uses ES256 (P-256 curve), not RS256 | java-jwt needs ES256 algorithm setup | java-jwt 4.4 supports ES256 via `Algorithm.ECDSA256(...)`. Public key parsing via `KeyFactory.getInstance("EC")`. |
| OAuth client id leaks in logs | Security smell, not a vuln per se | Client id is public; never log raw id tokens; document in code comments. |
