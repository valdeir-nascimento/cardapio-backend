# Phase 1.B — Identity Context Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the `identity` bounded context — Customer registration & login, Admin login, JWT access + refresh tokens, Spring Security wired with role-based authorization, `/me` profile endpoint.

**Architecture:** Clean Architecture inside the `com.cardapio.identity` module — POJO domain (no Spring/JPA), application services returning `Result<T>` with `Notification`, infrastructure adapters for JPA + BCrypt + JJWT. Spring Security validates JWTs via a custom filter; `@PreAuthorize` on admin endpoints. Refresh tokens persisted with revocation support. Customer JWT has `aud=customer`; admin JWT has `aud=admin` + `roles=[OWNER|MANAGER|OPERATOR]`.

**Tech Stack:** Adds `spring-boot-starter-security` and `io.jsonwebtoken:jjwt` (api/impl/jackson, 0.12.x). Reuses everything from Phase 1.A.

**Reference:**
- Spec: [docs/superpowers/specs/2026-05-04-cardapio-digital-backend-design.md](../specs/2026-05-04-cardapio-digital-backend-design.md) (Section 3 `identity`, Section 7.1 Auth, Section 8 Persistence)
- Phase 1.A plan: [docs/superpowers/plans/2026-05-04-phase-1a-foundation.md](./2026-05-04-phase-1a-foundation.md)

**Out of scope (deferred to later phases):**
- Social login (Google/Apple) → Phase 6
- Multiple addresses CRUD → Phase 6
- LGPD endpoints (`DELETE /me`, `GET /me/data-export`) → Phase 7
- Admin user management UI → Phase 7

---

## File Structure

```
src/main/java/com/cardapio/identity/
├── domain/
│   ├── model/
│   │   ├── Customer.java                   (aggregate root)
│   │   ├── CustomerId.java                 (typed UUID VO)
│   │   ├── Admin.java                      (aggregate root)
│   │   ├── AdminId.java                    (typed UUID VO)
│   │   ├── Role.java                       (enum)
│   │   ├── RefreshToken.java               (aggregate root)
│   │   ├── RefreshTokenId.java             (typed UUID VO)
│   │   ├── HashedPassword.java             (VO)
│   │   ├── RawPassword.java                (VO with strength validation)
│   │   ├── TokenPair.java                  (record: access + refresh)
│   │   └── Audience.java                   (enum: CUSTOMER, ADMIN)
│   ├── port/
│   │   ├── CustomerRepository.java
│   │   ├── AdminRepository.java
│   │   ├── RefreshTokenRepository.java
│   │   ├── PasswordHasher.java
│   │   ├── JwtIssuer.java
│   │   └── JwtVerifier.java
│   └── exception/
│       ├── EmailAlreadyRegisteredException.java
│       └── InvalidCredentialsException.java
├── application/
│   ├── command/
│   │   ├── RegisterCustomerCommand.java
│   │   ├── LoginCommand.java
│   │   ├── RefreshTokenCommand.java
│   │   └── UpdateProfileCommand.java
│   ├── query/
│   │   └── GetMyProfileQuery.java
│   ├── usecase/
│   │   ├── RegisterCustomerUseCase.java
│   │   ├── LoginCustomerUseCase.java
│   │   ├── LoginAdminUseCase.java
│   │   ├── RefreshTokenUseCase.java
│   │   ├── GetMyProfileUseCase.java
│   │   └── UpdateMyProfileUseCase.java
│   └── dto/
│       ├── CustomerProfile.java
│       └── AuthenticatedPrincipal.java
├── infrastructure/
│   ├── persistence/
│   │   ├── jpa/
│   │   │   ├── CustomerJpaEntity.java
│   │   │   ├── AdminJpaEntity.java
│   │   │   └── RefreshTokenJpaEntity.java
│   │   ├── repository/
│   │   │   ├── SpringCustomerJpaRepository.java
│   │   │   ├── SpringAdminJpaRepository.java
│   │   │   └── SpringRefreshTokenJpaRepository.java
│   │   ├── mapper/
│   │   │   ├── CustomerMapper.java
│   │   │   ├── AdminMapper.java
│   │   │   └── RefreshTokenMapper.java
│   │   └── adapter/
│   │       ├── CustomerRepositoryAdapter.java
│   │       ├── AdminRepositoryAdapter.java
│   │       └── RefreshTokenRepositoryAdapter.java
│   ├── security/
│   │   ├── BCryptPasswordHasher.java
│   │   └── JjwtAdapter.java                (implements both JwtIssuer + JwtVerifier)
│   └── seed/
│       └── DevAdminSeeder.java             (dev profile only)
├── api/
│   ├── rest/
│   │   ├── CustomerAuthController.java
│   │   ├── AdminAuthController.java
│   │   └── MeController.java
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── RefreshRequest.java
│   │   ├── TokenPairResponse.java
│   │   ├── ProfileResponse.java
│   │   └── UpdateProfileRequest.java
│   └── security/
│       ├── SecurityConfig.java
│       ├── JwtAuthenticationFilter.java
│       └── CardapioPrincipal.java
└── package-info.java                       (@ApplicationModule)

src/main/resources/db/migration/
├── V3__identity_tables.sql
└── V4__dev_admin_seed.sql                  (dev only — wrapped in DO block)

src/test/java/com/cardapio/identity/        (mirrors main structure)
└── ... unit + integration tests

src/main/resources/application-dev.yml      (modify: add JWT secret)
src/main/resources/application-test.yml     (modify: add JWT secret)
src/main/resources/application-staging.yml  (modify: add JWT env var)
src/main/resources/application-prod.yml     (modify: add JWT env var)
pom.xml                                     (modify: add Security + JJWT)
```

---

## Task 1: Add dependencies + identity module skeleton

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/com/cardapio/identity/package-info.java`

- [ ] **Step 1.1: Add Spring Security + JJWT to `pom.xml`**

In `pom.xml`, add a property for JJWT and 4 new dependencies (after the existing data-jpa entry, before the test deps):

```xml
<properties>
    <!-- existing properties -->
    <jjwt.version>0.12.6</jjwt.version>
</properties>
```

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>${jjwt.version}</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>${jjwt.version}</version>
    <scope>runtime</scope>
</dependency>
```

Also add `spring-security-test` to test deps:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 1.2: Create identity module package-info**

File: `src/main/java/com/cardapio/identity/package-info.java`

```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Identity",
    allowedDependencies = "shared"
)
package com.cardapio.identity;
```

- [ ] **Step 1.3: Verify build still passes**

Run: `./mvnw -DskipTests package`
Expected: BUILD SUCCESS.

NOTE: Adding `spring-boot-starter-security` will make all existing endpoints require auth at runtime (Spring Security defaults). The existing `HealthSmokeTest` may now return 401. If `./mvnw test` fails on `HealthSmokeTest` because `/actuator/health` returns 401, that's expected — Task 16 (Security config) will permit it. For now, expect 1-2 test regressions; we'll fix them in Task 16.

Actually, run a quick check: `./mvnw test -Dtest=HealthSmokeTest`. If it fails with 401, note that and proceed. Don't fix it now.

- [ ] **Step 1.4: Commit**

```bash
git add pom.xml src/main/java/com/cardapio/identity/package-info.java
git commit -m "chore(identity): add Spring Security + JJWT deps and module skeleton"
```

---

## Task 2: Identity value objects

**Files:**
- Create: `src/main/java/com/cardapio/identity/domain/model/CustomerId.java`
- Create: `src/main/java/com/cardapio/identity/domain/model/AdminId.java`
- Create: `src/main/java/com/cardapio/identity/domain/model/RefreshTokenId.java`
- Create: `src/main/java/com/cardapio/identity/domain/model/Role.java`
- Create: `src/main/java/com/cardapio/identity/domain/model/Audience.java`
- Create: `src/main/java/com/cardapio/identity/domain/model/RawPassword.java`
- Create: `src/main/java/com/cardapio/identity/domain/model/HashedPassword.java`
- Create: `src/main/java/com/cardapio/identity/domain/model/TokenPair.java`
- Test: `src/test/java/com/cardapio/identity/domain/model/RawPasswordTest.java`

- [ ] **Step 2.1: Write `RawPasswordTest` (TDD red)**

File: `src/test/java/com/cardapio/identity/domain/model/RawPasswordTest.java`

```java
package com.cardapio.identity.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawPasswordTest {

    @Test
    void acceptsStrongPassword() {
        RawPassword p = RawPassword.of("S3curePass!");
        assertThat(p.value()).isEqualTo("S3curePass!");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "short1!",         // < 8 chars
        "alllowercase1!",  // no uppercase
        "ALLUPPERCASE1!",  // no lowercase
        "NoNumbers!",      // no digit
        "NoSymbols123",    // no symbol
        ""                 // empty
    })
    void rejectsWeakPassword(String weak) {
        assertThatThrownBy(() -> RawPassword.of(weak))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("password");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> RawPassword.of(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toStringDoesNotExposeValue() {
        RawPassword p = RawPassword.of("S3curePass!");
        assertThat(p.toString()).doesNotContain("S3curePass!");
    }
}
```

- [ ] **Step 2.2: Run to verify it fails**

Run: `./mvnw test -Dtest=RawPasswordTest`
Expected: compilation failure — `RawPassword` doesn't exist.

- [ ] **Step 2.3: Implement value objects**

File: `src/main/java/com/cardapio/identity/domain/model/CustomerId.java`

```java
package com.cardapio.identity.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CustomerId(UUID value) {
    public CustomerId { Objects.requireNonNull(value, "value"); }
    public static CustomerId newId() { return new CustomerId(UUID.randomUUID()); }
    public static CustomerId of(UUID value) { return new CustomerId(value); }
    public static CustomerId of(String value) { return new CustomerId(UUID.fromString(value)); }
}
```

File: `src/main/java/com/cardapio/identity/domain/model/AdminId.java`

```java
package com.cardapio.identity.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AdminId(UUID value) {
    public AdminId { Objects.requireNonNull(value, "value"); }
    public static AdminId newId() { return new AdminId(UUID.randomUUID()); }
    public static AdminId of(UUID value) { return new AdminId(value); }
    public static AdminId of(String value) { return new AdminId(UUID.fromString(value)); }
}
```

File: `src/main/java/com/cardapio/identity/domain/model/RefreshTokenId.java`

```java
package com.cardapio.identity.domain.model;

import java.util.Objects;
import java.util.UUID;

public record RefreshTokenId(UUID value) {
    public RefreshTokenId { Objects.requireNonNull(value, "value"); }
    public static RefreshTokenId newId() { return new RefreshTokenId(UUID.randomUUID()); }
    public static RefreshTokenId of(UUID value) { return new RefreshTokenId(value); }
}
```

File: `src/main/java/com/cardapio/identity/domain/model/Role.java`

```java
package com.cardapio.identity.domain.model;

public enum Role {
    OWNER, MANAGER, OPERATOR
}
```

File: `src/main/java/com/cardapio/identity/domain/model/Audience.java`

```java
package com.cardapio.identity.domain.model;

public enum Audience {
    CUSTOMER, ADMIN
}
```

File: `src/main/java/com/cardapio/identity/domain/model/RawPassword.java`

```java
package com.cardapio.identity.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public final class RawPassword {

    private static final Pattern HAS_LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_SYMBOL = Pattern.compile(".*[^A-Za-z0-9].*");

    private final String value;

    private RawPassword(String value) {
        Objects.requireNonNull(value, "value");
        if (value.length() < 8
            || !HAS_LOWER.matcher(value).matches()
            || !HAS_UPPER.matcher(value).matches()
            || !HAS_DIGIT.matcher(value).matches()
            || !HAS_SYMBOL.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "weak password: must be ≥8 chars with upper, lower, digit and symbol");
        }
        this.value = value;
    }

    public static RawPassword of(String raw) { return new RawPassword(raw); }

    public String value() { return value; }

    @Override public String toString() { return "RawPassword[***]"; }
}
```

File: `src/main/java/com/cardapio/identity/domain/model/HashedPassword.java`

```java
package com.cardapio.identity.domain.model;

import java.util.Objects;

public record HashedPassword(String value) {
    public HashedPassword {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("hashed password must not be blank");
        }
    }
    @Override public String toString() { return "HashedPassword[***]"; }
}
```

File: `src/main/java/com/cardapio/identity/domain/model/TokenPair.java`

```java
package com.cardapio.identity.domain.model;

import java.time.Instant;
import java.util.Objects;

public record TokenPair(
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken,
    Instant refreshTokenExpiresAt
) {
    public TokenPair {
        Objects.requireNonNull(accessToken, "accessToken");
        Objects.requireNonNull(accessTokenExpiresAt, "accessTokenExpiresAt");
        Objects.requireNonNull(refreshToken, "refreshToken");
        Objects.requireNonNull(refreshTokenExpiresAt, "refreshTokenExpiresAt");
    }
}
```

- [ ] **Step 2.4: Run test to verify it passes**

Run: `./mvnw test -Dtest=RawPasswordTest`
Expected: PASS — 9 tests (1 + 6 parameterized + 2 others).

- [ ] **Step 2.5: Commit**

```bash
git add src/main/java/com/cardapio/identity/domain/model src/test/java/com/cardapio/identity/domain/model
git commit -m "feat(identity): add value objects (ids, role, audience, password, token pair)"
```

---

## Task 3: Customer aggregate

**Files:**
- Create: `src/main/java/com/cardapio/identity/domain/model/Customer.java`
- Create: `src/main/java/com/cardapio/identity/domain/exception/EmailAlreadyRegisteredException.java`
- Create: `src/main/java/com/cardapio/identity/domain/event/CustomerRegistered.java`
- Test: `src/test/java/com/cardapio/identity/domain/model/CustomerTest.java`

- [ ] **Step 3.1: Write `CustomerTest`**

File: `src/test/java/com/cardapio/identity/domain/model/CustomerTest.java`

```java
package com.cardapio.identity.domain.model;

import com.cardapio.identity.domain.event.CustomerRegistered;
import com.cardapio.shared.domain.DomainEvent;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTest {

    @Test
    void registersANewCustomerEmittingEvent() {
        Customer customer = Customer.register(
            "Maria Silva",
            Email.of("maria@example.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$dummyhash"));

        assertThat(customer.id()).isNotNull();
        assertThat(customer.name()).isEqualTo("Maria Silva");
        assertThat(customer.email().value()).isEqualTo("maria@example.com");

        List<DomainEvent> events = customer.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(CustomerRegistered.class);
    }

    @Test
    void changesNameAndPhone() {
        Customer customer = Customer.register(
            "Old Name",
            Email.of("a@b.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$x"));
        customer.pullDomainEvents();  // drain registration event

        customer.updateProfile("New Name", PhoneNumber.of("+5511987654321"));

        assertThat(customer.name()).isEqualTo("New Name");
        assertThat(customer.phoneNumber().value()).isEqualTo("+5511987654321");
    }

    @Test
    void rehydratesFromPersistence() {
        CustomerId id = CustomerId.newId();
        Customer customer = Customer.rehydrate(
            id,
            "Maria",
            Email.of("maria@example.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$x"));

        assertThat(customer.id()).isEqualTo(id);
        assertThat(customer.pullDomainEvents()).isEmpty();  // no event on rehydrate
    }
}
```

- [ ] **Step 3.2: Run, see fail (compile errors)**

Run: `./mvnw test -Dtest=CustomerTest`
Expected: compilation failure.

- [ ] **Step 3.3: Implement `CustomerRegistered` event**

File: `src/main/java/com/cardapio/identity/domain/event/CustomerRegistered.java`

```java
package com.cardapio.identity.domain.event;

import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.shared.domain.DomainEvent;
import com.cardapio.shared.domain.Email;

import java.time.Instant;
import java.util.UUID;

public record CustomerRegistered(
    UUID id,
    Instant occurredOn,
    CustomerId customerId,
    Email email
) implements DomainEvent {

    public static CustomerRegistered now(CustomerId customerId, Email email) {
        return new CustomerRegistered(UUID.randomUUID(), Instant.now(), customerId, email);
    }
}
```

- [ ] **Step 3.4: Implement `EmailAlreadyRegisteredException`**

File: `src/main/java/com/cardapio/identity/domain/exception/EmailAlreadyRegisteredException.java`

```java
package com.cardapio.identity.domain.exception;

import com.cardapio.shared.domain.DomainException;
import com.cardapio.shared.domain.Email;

public class EmailAlreadyRegisteredException extends DomainException {
    public EmailAlreadyRegisteredException(Email email) {
        super("EMAIL_ALREADY_REGISTERED", "email already registered: " + email.value());
    }
}
```

- [ ] **Step 3.5: Implement `Customer` aggregate**

File: `src/main/java/com/cardapio/identity/domain/model/Customer.java`

```java
package com.cardapio.identity.domain.model;

import com.cardapio.identity.domain.event.CustomerRegistered;
import com.cardapio.shared.domain.AggregateRoot;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;

import java.util.Objects;

public final class Customer extends AggregateRoot<CustomerId> {

    private String name;
    private final Email email;
    private PhoneNumber phoneNumber;
    private HashedPassword passwordHash;

    private Customer(CustomerId id, String name, Email email, PhoneNumber phoneNumber, HashedPassword passwordHash) {
        super(id);
        this.name = Objects.requireNonNull(name, "name").trim();
        if (this.name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        this.email = Objects.requireNonNull(email, "email");
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    }

    public static Customer register(String name, Email email, PhoneNumber phoneNumber, HashedPassword passwordHash) {
        Customer c = new Customer(CustomerId.newId(), name, email, phoneNumber, passwordHash);
        c.registerEvent(CustomerRegistered.now(c.id(), c.email));
        return c;
    }

    public static Customer rehydrate(CustomerId id, String name, Email email, PhoneNumber phoneNumber, HashedPassword passwordHash) {
        return new Customer(id, name, email, phoneNumber, passwordHash);
    }

    public void updateProfile(String name, PhoneNumber phoneNumber) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(phoneNumber, "phoneNumber");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("name must not be blank");
        this.name = trimmed;
        this.phoneNumber = phoneNumber;
    }

    public void changePassword(HashedPassword newHash) {
        this.passwordHash = Objects.requireNonNull(newHash, "newHash");
    }

    public String name() { return name; }
    public Email email() { return email; }
    public PhoneNumber phoneNumber() { return phoneNumber; }
    public HashedPassword passwordHash() { return passwordHash; }
}
```

- [ ] **Step 3.6: Run test to verify it passes**

Run: `./mvnw test -Dtest=CustomerTest`
Expected: PASS — 3 tests.

- [ ] **Step 3.7: Commit**

```bash
git add src/main/java/com/cardapio/identity/domain/model/Customer.java \
        src/main/java/com/cardapio/identity/domain/exception \
        src/main/java/com/cardapio/identity/domain/event \
        src/test/java/com/cardapio/identity/domain/model/CustomerTest.java
git commit -m "feat(identity): add Customer aggregate with registration event"
```

---

## Task 4: Admin aggregate

**Files:**
- Create: `src/main/java/com/cardapio/identity/domain/model/Admin.java`
- Test: `src/test/java/com/cardapio/identity/domain/model/AdminTest.java`

- [ ] **Step 4.1: Write `AdminTest`**

File: `src/test/java/com/cardapio/identity/domain/model/AdminTest.java`

```java
package com.cardapio.identity.domain.model;

import com.cardapio.shared.domain.Email;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminTest {

    @Test
    void createsAdminWithRoles() {
        Admin admin = Admin.create(
            "Boss",
            Email.of("boss@cardapio.com"),
            HashedPassword.of("$2a$12$x"),
            Set.of(Role.OWNER));

        assertThat(admin.name()).isEqualTo("Boss");
        assertThat(admin.hasRole(Role.OWNER)).isTrue();
        assertThat(admin.hasRole(Role.OPERATOR)).isFalse();
    }

    @Test
    void rolesAreImmutableFromOutside() {
        Admin admin = Admin.create(
            "X", Email.of("x@y.com"), HashedPassword.of("$2a$12$x"), Set.of(Role.MANAGER));

        Set<Role> roles = admin.roles();
        assertThatThrownBy(() -> roles.add(Role.OWNER))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsEmptyRoles() {
        assertThatThrownBy(() -> Admin.create(
            "X", Email.of("x@y.com"), HashedPassword.of("$2a$12$x"), Set.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one role");
    }
}
```

- [ ] **Step 4.2: Run, fail.**

Run: `./mvnw test -Dtest=AdminTest`
Expected: compile failure.

- [ ] **Step 4.3: Implement `Admin`**

File: `src/main/java/com/cardapio/identity/domain/model/Admin.java`

```java
package com.cardapio.identity.domain.model;

import com.cardapio.shared.domain.AggregateRoot;
import com.cardapio.shared.domain.Email;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class Admin extends AggregateRoot<AdminId> {

    private String name;
    private final Email email;
    private HashedPassword passwordHash;
    private final EnumSet<Role> roles;

    private Admin(AdminId id, String name, Email email, HashedPassword passwordHash, Set<Role> roles) {
        super(id);
        this.name = Objects.requireNonNull(name, "name").trim();
        if (this.name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(roles, "roles");
        if (roles.isEmpty()) throw new IllegalArgumentException("admin must have at least one role");
        this.roles = EnumSet.copyOf(roles);
    }

    public static Admin create(String name, Email email, HashedPassword passwordHash, Set<Role> roles) {
        return new Admin(AdminId.newId(), name, email, passwordHash, roles);
    }

    public static Admin rehydrate(AdminId id, String name, Email email, HashedPassword passwordHash, Set<Role> roles) {
        return new Admin(id, name, email, passwordHash, roles);
    }

    public boolean hasRole(Role role) { return roles.contains(role); }
    public Set<Role> roles() { return java.util.Collections.unmodifiableSet(roles); }
    public String name() { return name; }
    public Email email() { return email; }
    public HashedPassword passwordHash() { return passwordHash; }
}
```

- [ ] **Step 4.4: Run, pass, commit.**

```bash
./mvnw test -Dtest=AdminTest
git add src/main/java/com/cardapio/identity/domain/model/Admin.java src/test/java/com/cardapio/identity/domain/model/AdminTest.java
git commit -m "feat(identity): add Admin aggregate with roles"
```

---

## Task 5: RefreshToken aggregate

**Files:**
- Create: `src/main/java/com/cardapio/identity/domain/model/RefreshToken.java`
- Test: `src/test/java/com/cardapio/identity/domain/model/RefreshTokenTest.java`

- [ ] **Step 5.1: Write `RefreshTokenTest`**

File: `src/test/java/com/cardapio/identity/domain/model/RefreshTokenTest.java`

```java
package com.cardapio.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void issuesActiveTokenForCustomer() {
        UUID subject = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-04T10:00:00Z");
        RefreshToken token = RefreshToken.issue(
            subject, Audience.CUSTOMER, "hashed-token", now, Duration.ofDays(30));

        assertThat(token.subject()).isEqualTo(subject);
        assertThat(token.audience()).isEqualTo(Audience.CUSTOMER);
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.expiresAt()).isEqualTo(now.plus(Duration.ofDays(30)));
        assertThat(token.isActiveAt(now.plusSeconds(60))).isTrue();
    }

    @Test
    void detectsExpiry() {
        Instant now = Instant.parse("2026-05-04T10:00:00Z");
        RefreshToken token = RefreshToken.issue(
            UUID.randomUUID(), Audience.CUSTOMER, "h", now, Duration.ofMinutes(5));

        assertThat(token.isActiveAt(now.plusSeconds(60))).isTrue();
        assertThat(token.isActiveAt(now.plus(Duration.ofMinutes(10)))).isFalse();
    }

    @Test
    void revokeMakesTokenInactive() {
        Instant now = Instant.parse("2026-05-04T10:00:00Z");
        RefreshToken token = RefreshToken.issue(
            UUID.randomUUID(), Audience.CUSTOMER, "h", now, Duration.ofDays(30));

        token.revoke();

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isActiveAt(now.plusSeconds(60))).isFalse();
    }
}
```

- [ ] **Step 5.2: Implement `RefreshToken`**

File: `src/main/java/com/cardapio/identity/domain/model/RefreshToken.java`

```java
package com.cardapio.identity.domain.model;

import com.cardapio.shared.domain.AggregateRoot;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class RefreshToken extends AggregateRoot<RefreshTokenId> {

    private final UUID subject;
    private final Audience audience;
    private final String hashedToken;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private boolean revoked;

    private RefreshToken(RefreshTokenId id, UUID subject, Audience audience,
                         String hashedToken, Instant issuedAt, Instant expiresAt, boolean revoked) {
        super(id);
        this.subject = Objects.requireNonNull(subject, "subject");
        this.audience = Objects.requireNonNull(audience, "audience");
        this.hashedToken = Objects.requireNonNull(hashedToken, "hashedToken");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.revoked = revoked;
    }

    public static RefreshToken issue(UUID subject, Audience audience, String hashedToken,
                                     Instant now, Duration validity) {
        return new RefreshToken(RefreshTokenId.newId(), subject, audience, hashedToken,
            now, now.plus(validity), false);
    }

    public static RefreshToken rehydrate(RefreshTokenId id, UUID subject, Audience audience,
                                         String hashedToken, Instant issuedAt, Instant expiresAt, boolean revoked) {
        return new RefreshToken(id, subject, audience, hashedToken, issuedAt, expiresAt, revoked);
    }

    public void revoke() { this.revoked = true; }

    public boolean isActiveAt(Instant when) {
        return !revoked && when.isBefore(expiresAt);
    }

    public boolean isRevoked() { return revoked; }
    public UUID subject() { return subject; }
    public Audience audience() { return audience; }
    public String hashedToken() { return hashedToken; }
    public Instant issuedAt() { return issuedAt; }
    public Instant expiresAt() { return expiresAt; }
}
```

- [ ] **Step 5.3: Run, pass, commit.**

```bash
./mvnw test -Dtest=RefreshTokenTest
git add src/main/java/com/cardapio/identity/domain/model/RefreshToken.java src/test/java/com/cardapio/identity/domain/model/RefreshTokenTest.java
git commit -m "feat(identity): add RefreshToken aggregate with revocation"
```

---

## Task 6: Domain ports

**Files (all in `src/main/java/com/cardapio/identity/domain/port/`):**
- `CustomerRepository.java`
- `AdminRepository.java`
- `RefreshTokenRepository.java`
- `PasswordHasher.java`
- `JwtIssuer.java`
- `JwtVerifier.java`

- [ ] **Step 6.1: Create `CustomerRepository`**

```java
package com.cardapio.identity.domain.port;

import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.shared.domain.Email;

import java.util.Optional;

public interface CustomerRepository {
    void save(Customer customer);
    Optional<Customer> findById(CustomerId id);
    Optional<Customer> findByEmail(Email email);
    boolean existsByEmail(Email email);
}
```

- [ ] **Step 6.2: Create `AdminRepository`**

```java
package com.cardapio.identity.domain.port;

import com.cardapio.identity.domain.model.Admin;
import com.cardapio.identity.domain.model.AdminId;
import com.cardapio.shared.domain.Email;

import java.util.Optional;

public interface AdminRepository {
    void save(Admin admin);
    Optional<Admin> findById(AdminId id);
    Optional<Admin> findByEmail(Email email);
}
```

- [ ] **Step 6.3: Create `RefreshTokenRepository`**

```java
package com.cardapio.identity.domain.port;

import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.RefreshTokenId;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    void save(RefreshToken token);
    Optional<RefreshToken> findById(RefreshTokenId id);
    Optional<RefreshToken> findByHashedToken(String hashedToken);
    void revokeAllForSubject(UUID subject);
}
```

- [ ] **Step 6.4: Create `PasswordHasher`**

```java
package com.cardapio.identity.domain.port;

import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.RawPassword;

public interface PasswordHasher {
    HashedPassword hash(RawPassword raw);
    boolean matches(RawPassword raw, HashedPassword hash);
}
```

- [ ] **Step 6.5: Create `JwtIssuer`**

```java
package com.cardapio.identity.domain.port;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface JwtIssuer {
    IssuedJwt issueAccessToken(UUID subject, Audience audience, Set<Role> roles);
    String generateOpaqueRefreshToken();   // returns the raw refresh token (random URL-safe string)
    Instant accessTokenExpiry(Instant now);
    Instant refreshTokenExpiry(Instant now);

    record IssuedJwt(String token, Instant expiresAt) {}
}
```

- [ ] **Step 6.6: Create `JwtVerifier`**

```java
package com.cardapio.identity.domain.port;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Role;

import java.util.Set;
import java.util.UUID;

public interface JwtVerifier {
    VerifiedJwt verify(String token);

    record VerifiedJwt(UUID subject, Audience audience, Set<Role> roles) {}
}
```

- [ ] **Step 6.7: Verify the project still compiles**

Run: `./mvnw -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6.8: Commit**

```bash
git add src/main/java/com/cardapio/identity/domain/port
git commit -m "feat(identity): add domain ports (repositories, hasher, JWT issuer/verifier)"
```

---

## Task 7: BCrypt password hasher adapter

**Files:**
- Create: `src/main/java/com/cardapio/identity/infrastructure/security/BCryptPasswordHasher.java`
- Test: `src/test/java/com/cardapio/identity/infrastructure/security/BCryptPasswordHasherTest.java`

- [ ] **Step 7.1: Test**

File: `src/test/java/com/cardapio/identity/infrastructure/security/BCryptPasswordHasherTest.java`

```java
package com.cardapio.identity.infrastructure.security;

import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.RawPassword;
import com.cardapio.identity.domain.port.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final PasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hashAndMatch() {
        RawPassword raw = RawPassword.of("S3cret!Password");
        HashedPassword hash = hasher.hash(raw);

        assertThat(hash.value()).startsWith("$2");
        assertThat(hasher.matches(raw, hash)).isTrue();
        assertThat(hasher.matches(RawPassword.of("Wrong!Pass1"), hash)).isFalse();
    }

    @Test
    void differentHashesEachTime() {
        RawPassword raw = RawPassword.of("S3cret!Password");
        HashedPassword h1 = hasher.hash(raw);
        HashedPassword h2 = hasher.hash(raw);
        assertThat(h1.value()).isNotEqualTo(h2.value());
        assertThat(hasher.matches(raw, h1)).isTrue();
        assertThat(hasher.matches(raw, h2)).isTrue();
    }
}
```

- [ ] **Step 7.2: Implement**

File: `src/main/java/com/cardapio/identity/infrastructure/security/BCryptPasswordHasher.java`

```java
package com.cardapio.identity.infrastructure.security;

import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.RawPassword;
import com.cardapio.identity.domain.port.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public HashedPassword hash(RawPassword raw) {
        return new HashedPassword(encoder.encode(raw.value()));
    }

    @Override
    public boolean matches(RawPassword raw, HashedPassword hash) {
        return encoder.matches(raw.value(), hash.value());
    }
}
```

- [ ] **Step 7.3: Run, pass, commit.**

```bash
./mvnw test -Dtest=BCryptPasswordHasherTest
git add src/main/java/com/cardapio/identity/infrastructure/security src/test/java/com/cardapio/identity/infrastructure/security
git commit -m "feat(identity): add BCrypt password hasher adapter"
```

---

## Task 8: JJWT adapter (issuer + verifier)

**Files:**
- Create: `src/main/java/com/cardapio/identity/infrastructure/security/JjwtAdapter.java`
- Create: `src/main/java/com/cardapio/identity/infrastructure/security/JwtProperties.java`
- Test: `src/test/java/com/cardapio/identity/infrastructure/security/JjwtAdapterTest.java`

- [ ] **Step 8.1: Test**

File: `src/test/java/com/cardapio/identity/infrastructure/security/JjwtAdapterTest.java`

```java
package com.cardapio.identity.infrastructure.security;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Role;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.JwtVerifier;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JjwtAdapterTest {

    private final JwtProperties props = new JwtProperties(
        "test-secret-must-be-long-enough-for-hs256-32bytes!!",
        Duration.ofMinutes(15),
        Duration.ofDays(30));
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-04T10:00:00Z"), ZoneId.of("UTC"));
    private final JjwtAdapter adapter = new JjwtAdapter(props, fixedClock);

    @Test
    void issuesAndVerifiesCustomerToken() {
        UUID subject = UUID.randomUUID();
        JwtIssuer.IssuedJwt issued = adapter.issueAccessToken(subject, Audience.CUSTOMER, Set.of());

        JwtVerifier.VerifiedJwt verified = adapter.verify(issued.token());

        assertThat(verified.subject()).isEqualTo(subject);
        assertThat(verified.audience()).isEqualTo(Audience.CUSTOMER);
        assertThat(verified.roles()).isEmpty();
    }

    @Test
    void issuesAdminTokenWithRoles() {
        UUID subject = UUID.randomUUID();
        JwtIssuer.IssuedJwt issued = adapter.issueAccessToken(subject, Audience.ADMIN, Set.of(Role.OWNER, Role.MANAGER));

        JwtVerifier.VerifiedJwt verified = adapter.verify(issued.token());

        assertThat(verified.audience()).isEqualTo(Audience.ADMIN);
        assertThat(verified.roles()).containsExactlyInAnyOrder(Role.OWNER, Role.MANAGER);
    }

    @Test
    void rejectsTamperedToken() {
        JwtIssuer.IssuedJwt issued = adapter.issueAccessToken(UUID.randomUUID(), Audience.CUSTOMER, Set.of());
        String tampered = issued.token().substring(0, issued.token().length() - 4) + "XXXX";

        assertThatThrownBy(() -> adapter.verify(tampered))
            .hasMessageContaining("invalid");
    }

    @Test
    void generatesUniqueRefreshTokens() {
        String t1 = adapter.generateOpaqueRefreshToken();
        String t2 = adapter.generateOpaqueRefreshToken();
        assertThat(t1).isNotEqualTo(t2);
        assertThat(t1.length()).isGreaterThanOrEqualTo(43);  // 32 bytes base64url ≈ 43 chars
    }
}
```

- [ ] **Step 8.2: Implement `JwtProperties`**

File: `src/main/java/com/cardapio/identity/infrastructure/security/JwtProperties.java`

```java
package com.cardapio.identity.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cardapio.security.jwt")
public record JwtProperties(
    String secret,
    Duration accessTokenTtl,
    Duration refreshTokenTtl
) {
    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be ≥32 characters");
        }
        if (accessTokenTtl == null) accessTokenTtl = Duration.ofMinutes(15);
        if (refreshTokenTtl == null) refreshTokenTtl = Duration.ofDays(30);
    }
}
```

- [ ] **Step 8.3: Implement `JjwtAdapter`**

File: `src/main/java/com/cardapio/identity/infrastructure/security/JjwtAdapter.java`

```java
package com.cardapio.identity.infrastructure.security;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Role;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.JwtVerifier;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JjwtAdapter implements JwtIssuer, JwtVerifier {

    private final JwtProperties props;
    private final Clock clock;
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public JjwtAdapter(JwtProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IssuedJwt issueAccessToken(UUID subject, Audience audience, Set<Role> roles) {
        Instant now = clock.instant();
        Instant exp = now.plus(props.accessTokenTtl());
        String token = Jwts.builder()
            .subject(subject.toString())
            .audience().add(audience.name()).and()
            .claim("roles", roles.stream().map(Role::name).toList())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(key, Jwts.SIG.HS256)
            .compact();
        return new IssuedJwt(token, exp);
    }

    @Override
    public String generateOpaqueRefreshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public Instant accessTokenExpiry(Instant now) { return now.plus(props.accessTokenTtl()); }

    @Override
    public Instant refreshTokenExpiry(Instant now) { return now.plus(props.refreshTokenTtl()); }

    @Override
    public VerifiedJwt verify(String token) {
        try {
            var claims = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

            UUID subject = UUID.fromString(claims.getSubject());
            Audience audience = Audience.valueOf(claims.getAudience().iterator().next());
            @SuppressWarnings("unchecked")
            List<String> roleNames = (List<String>) claims.getOrDefault("roles", List.of());
            Set<Role> roles = roleNames.stream().map(Role::valueOf).collect(Collectors.toUnmodifiableSet());
            return new VerifiedJwt(subject, audience, roles);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("invalid JWT: " + e.getMessage(), e);
        }
    }
}
```

- [ ] **Step 8.4: Register `JwtProperties` in main app class**

Modify `src/main/java/com/cardapio/CardapioApplication.java`:

```java
package com.cardapio;

import com.cardapio.identity.infrastructure.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.Modulithic;

import java.time.Clock;

@Modulithic(systemName = "Cardapio Digital")
@SpringBootApplication
@ConfigurationPropertiesScan
public class CardapioApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardapioApplication.class, args);
    }

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
```

- [ ] **Step 8.5: Add JWT secret to test/dev profiles**

Update `src/test/resources/application-test.yml` (append):

```yaml
cardapio:
  security:
    jwt:
      secret: test-secret-must-be-long-enough-for-hs256-32bytes!!
      access-token-ttl: 15m
      refresh-token-ttl: 30d
```

Update `src/main/resources/application-dev.yml` (append):

```yaml
cardapio:
  security:
    jwt:
      secret: dev-secret-change-me-must-be-long-enough-32bytes!!
      access-token-ttl: 15m
      refresh-token-ttl: 30d
```

Update `src/main/resources/application-staging.yml` (append):

```yaml
cardapio:
  security:
    jwt:
      secret: ${JWT_SECRET}
      access-token-ttl: 15m
      refresh-token-ttl: 30d
```

Update `src/main/resources/application-prod.yml` (append):

```yaml
cardapio:
  security:
    jwt:
      secret: ${JWT_SECRET}
      access-token-ttl: 15m
      refresh-token-ttl: 30d
```

- [ ] **Step 8.6: Run, pass, commit.**

```bash
./mvnw test -Dtest=JjwtAdapterTest
git add src/main/java/com/cardapio/identity/infrastructure/security/JjwtAdapter.java \
        src/main/java/com/cardapio/identity/infrastructure/security/JwtProperties.java \
        src/main/java/com/cardapio/CardapioApplication.java \
        src/main/resources/application-dev.yml src/main/resources/application-staging.yml src/main/resources/application-prod.yml \
        src/test/resources/application-test.yml \
        src/test/java/com/cardapio/identity/infrastructure/security/JjwtAdapterTest.java
git commit -m "feat(identity): add JJWT adapter (issuer + verifier) with config properties"
```

---

## Task 9: Flyway migration for identity tables

**Files:**
- Create: `src/main/resources/db/migration/V3__identity_tables.sql`

- [ ] **Step 9.1: Write migration**

File: `src/main/resources/db/migration/V3__identity_tables.sql`

```sql
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_customers_email ON customers (email);

CREATE TABLE admins (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    roles VARCHAR(60) NOT NULL,  -- comma-separated: OWNER,MANAGER,OPERATOR
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_admins_email ON admins (email);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    subject UUID NOT NULL,
    audience VARCHAR(20) NOT NULL,
    hashed_token VARCHAR(120) NOT NULL UNIQUE,
    issued_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_refresh_tokens_subject ON refresh_tokens (subject);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens (hashed_token);
```

- [ ] **Step 9.2: Run integration test to apply migration via Testcontainers**

Run: `./mvnw test -Dtest=SmokeIntegrationTest`
Expected: PASS — Flyway applies V3 successfully alongside V1, V2.

- [ ] **Step 9.3: Commit**

```bash
git add src/main/resources/db/migration/V3__identity_tables.sql
git commit -m "feat(identity): add Flyway migration for customers, admins, refresh_tokens"
```

---

## Task 10: JPA persistence layer (entities + repositories + mappers + adapters)

**Files (all under `src/main/java/com/cardapio/identity/infrastructure/persistence/`):**
- `jpa/CustomerJpaEntity.java`, `jpa/AdminJpaEntity.java`, `jpa/RefreshTokenJpaEntity.java`
- `repository/SpringCustomerJpaRepository.java`, `repository/SpringAdminJpaRepository.java`, `repository/SpringRefreshTokenJpaRepository.java`
- `mapper/CustomerMapper.java`, `mapper/AdminMapper.java`, `mapper/RefreshTokenMapper.java`
- `adapter/CustomerRepositoryAdapter.java`, `adapter/AdminRepositoryAdapter.java`, `adapter/RefreshTokenRepositoryAdapter.java`

- [ ] **Step 10.1: `CustomerJpaEntity`**

File: `src/main/java/com/cardapio/identity/infrastructure/persistence/jpa/CustomerJpaEntity.java`

```java
package com.cardapio.identity.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class CustomerJpaEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 180, unique = true)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerJpaEntity() {}

    public CustomerJpaEntity(UUID id, String name, String email, String phoneNumber, String passwordHash, Instant createdAt, Instant updatedAt) {
        this.id = id; this.name = name; this.email = email; this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 10.2: `AdminJpaEntity`**

File: `src/main/java/com/cardapio/identity/infrastructure/persistence/jpa/AdminJpaEntity.java`

```java
package com.cardapio.identity.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admins")
public class AdminJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 180, unique = true) private String email;
    @Column(name = "password_hash", nullable = false, length = 120) private String passwordHash;
    @Column(nullable = false, length = 60) private String roles;  // comma-separated
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected AdminJpaEntity() {}

    public AdminJpaEntity(UUID id, String name, String email, String passwordHash, String roles, Instant createdAt) {
        this.id = id; this.name = name; this.email = email;
        this.passwordHash = passwordHash; this.roles = roles; this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRoles() { return roles; }
    public Instant getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRoles(String roles) { this.roles = roles; }
}
```

- [ ] **Step 10.3: `RefreshTokenJpaEntity`**

File: `src/main/java/com/cardapio/identity/infrastructure/persistence/jpa/RefreshTokenJpaEntity.java`

```java
package com.cardapio.identity.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(nullable = false) private UUID subject;
    @Column(nullable = false, length = 20) private String audience;
    @Column(name = "hashed_token", nullable = false, length = 120, unique = true) private String hashedToken;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(nullable = false) private boolean revoked;

    protected RefreshTokenJpaEntity() {}

    public RefreshTokenJpaEntity(UUID id, UUID subject, String audience, String hashedToken, Instant issuedAt, Instant expiresAt, boolean revoked) {
        this.id = id; this.subject = subject; this.audience = audience; this.hashedToken = hashedToken;
        this.issuedAt = issuedAt; this.expiresAt = expiresAt; this.revoked = revoked;
    }

    public UUID getId() { return id; }
    public UUID getSubject() { return subject; }
    public String getAudience() { return audience; }
    public String getHashedToken() { return hashedToken; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }

    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
```

- [ ] **Step 10.4: Spring Data JPA repositories**

File: `src/main/java/com/cardapio/identity/infrastructure/persistence/repository/SpringCustomerJpaRepository.java`

```java
package com.cardapio.identity.infrastructure.persistence.repository;

import com.cardapio.identity.infrastructure.persistence.jpa.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringCustomerJpaRepository extends JpaRepository<CustomerJpaEntity, UUID> {
    Optional<CustomerJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

File: `src/main/java/com/cardapio/identity/infrastructure/persistence/repository/SpringAdminJpaRepository.java`

```java
package com.cardapio.identity.infrastructure.persistence.repository;

import com.cardapio.identity.infrastructure.persistence.jpa.AdminJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringAdminJpaRepository extends JpaRepository<AdminJpaEntity, UUID> {
    Optional<AdminJpaEntity> findByEmail(String email);
}
```

File: `src/main/java/com/cardapio/identity/infrastructure/persistence/repository/SpringRefreshTokenJpaRepository.java`

```java
package com.cardapio.identity.infrastructure.persistence.repository;

import com.cardapio.identity.infrastructure.persistence.jpa.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringRefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {
    Optional<RefreshTokenJpaEntity> findByHashedToken(String hashedToken);

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revoked = true WHERE t.subject = :subject AND t.revoked = false")
    void revokeAllForSubject(@Param("subject") UUID subject);
}
```

- [ ] **Step 10.5: Mappers**

File: `src/main/java/com/cardapio/identity/infrastructure/persistence/mapper/CustomerMapper.java`

```java
package com.cardapio.identity.infrastructure.persistence.mapper;

import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.infrastructure.persistence.jpa.CustomerJpaEntity;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;

import java.time.Instant;

public final class CustomerMapper {
    private CustomerMapper() {}

    public static CustomerJpaEntity toJpa(Customer c, Instant now) {
        return new CustomerJpaEntity(
            c.id().value(), c.name(), c.email().value(), c.phoneNumber().value(),
            c.passwordHash().value(), now, now);
    }

    public static void updateJpa(CustomerJpaEntity entity, Customer c, Instant now) {
        entity.setName(c.name());
        entity.setPhoneNumber(c.phoneNumber().value());
        entity.setPasswordHash(c.passwordHash().value());
        entity.setUpdatedAt(now);
    }

    public static Customer toDomain(CustomerJpaEntity e) {
        return Customer.rehydrate(
            CustomerId.of(e.getId()),
            e.getName(),
            Email.of(e.getEmail()),
            PhoneNumber.of(e.getPhoneNumber()),
            new HashedPassword(e.getPasswordHash()));
    }
}
```

File: `src/main/java/com/cardapio/identity/infrastructure/persistence/mapper/AdminMapper.java`

```java
package com.cardapio.identity.infrastructure.persistence.mapper;

import com.cardapio.identity.domain.model.Admin;
import com.cardapio.identity.domain.model.AdminId;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.Role;
import com.cardapio.identity.infrastructure.persistence.jpa.AdminJpaEntity;
import com.cardapio.shared.domain.Email;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class AdminMapper {
    private AdminMapper() {}

    public static AdminJpaEntity toJpa(Admin a, Instant now) {
        String roles = a.roles().stream().map(Role::name).collect(Collectors.joining(","));
        return new AdminJpaEntity(a.id().value(), a.name(), a.email().value(), a.passwordHash().value(), roles, now);
    }

    public static void updateJpa(AdminJpaEntity entity, Admin a) {
        entity.setName(a.name());
        entity.setPasswordHash(a.passwordHash().value());
        entity.setRoles(a.roles().stream().map(Role::name).collect(Collectors.joining(",")));
    }

    public static Admin toDomain(AdminJpaEntity e) {
        Set<Role> roles = e.getRoles().isBlank()
            ? EnumSet.noneOf(Role.class)
            : Arrays.stream(e.getRoles().split(",")).map(Role::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
        return Admin.rehydrate(AdminId.of(e.getId()), e.getName(), Email.of(e.getEmail()), new HashedPassword(e.getPasswordHash()), roles);
    }
}
```

File: `src/main/java/com/cardapio/identity/infrastructure/persistence/mapper/RefreshTokenMapper.java`

```java
package com.cardapio.identity.infrastructure.persistence.mapper;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.RefreshTokenId;
import com.cardapio.identity.infrastructure.persistence.jpa.RefreshTokenJpaEntity;

public final class RefreshTokenMapper {
    private RefreshTokenMapper() {}

    public static RefreshTokenJpaEntity toJpa(RefreshToken t) {
        return new RefreshTokenJpaEntity(
            t.id().value(), t.subject(), t.audience().name(),
            t.hashedToken(), t.issuedAt(), t.expiresAt(), t.isRevoked());
    }

    public static void updateJpa(RefreshTokenJpaEntity entity, RefreshToken t) {
        entity.setRevoked(t.isRevoked());
    }

    public static RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return RefreshToken.rehydrate(
            RefreshTokenId.of(e.getId()), e.getSubject(),
            Audience.valueOf(e.getAudience()), e.getHashedToken(),
            e.getIssuedAt(), e.getExpiresAt(), e.isRevoked());
    }
}
```

- [ ] **Step 10.6: Repository adapters**

File: `src/main/java/com/cardapio/identity/infrastructure/persistence/adapter/CustomerRepositoryAdapter.java`

```java
package com.cardapio.identity.infrastructure.persistence.adapter;

import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.infrastructure.persistence.mapper.CustomerMapper;
import com.cardapio.identity.infrastructure.persistence.repository.SpringCustomerJpaRepository;
import com.cardapio.shared.domain.Email;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

@Component
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final SpringCustomerJpaRepository jpa;
    private final Clock clock;

    public CustomerRepositoryAdapter(SpringCustomerJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public void save(Customer customer) {
        var existing = jpa.findById(customer.id().value());
        if (existing.isPresent()) {
            CustomerMapper.updateJpa(existing.get(), customer, clock.instant());
            jpa.save(existing.get());
        } else {
            jpa.save(CustomerMapper.toJpa(customer, clock.instant()));
        }
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return jpa.findById(id.value()).map(CustomerMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(CustomerMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }
}
```

File: `src/main/java/com/cardapio/identity/infrastructure/persistence/adapter/AdminRepositoryAdapter.java`

```java
package com.cardapio.identity.infrastructure.persistence.adapter;

import com.cardapio.identity.domain.model.Admin;
import com.cardapio.identity.domain.model.AdminId;
import com.cardapio.identity.domain.port.AdminRepository;
import com.cardapio.identity.infrastructure.persistence.mapper.AdminMapper;
import com.cardapio.identity.infrastructure.persistence.repository.SpringAdminJpaRepository;
import com.cardapio.shared.domain.Email;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

@Component
public class AdminRepositoryAdapter implements AdminRepository {

    private final SpringAdminJpaRepository jpa;
    private final Clock clock;

    public AdminRepositoryAdapter(SpringAdminJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public void save(Admin admin) {
        var existing = jpa.findById(admin.id().value());
        if (existing.isPresent()) {
            AdminMapper.updateJpa(existing.get(), admin);
            jpa.save(existing.get());
        } else {
            jpa.save(AdminMapper.toJpa(admin, clock.instant()));
        }
    }

    @Override
    public Optional<Admin> findById(AdminId id) {
        return jpa.findById(id.value()).map(AdminMapper::toDomain);
    }

    @Override
    public Optional<Admin> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(AdminMapper::toDomain);
    }
}
```

File: `src/main/java/com/cardapio/identity/infrastructure/persistence/adapter/RefreshTokenRepositoryAdapter.java`

```java
package com.cardapio.identity.infrastructure.persistence.adapter;

import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.RefreshTokenId;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.identity.infrastructure.persistence.mapper.RefreshTokenMapper;
import com.cardapio.identity.infrastructure.persistence.repository.SpringRefreshTokenJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final SpringRefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(SpringRefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(RefreshToken token) {
        var existing = jpa.findById(token.id().value());
        if (existing.isPresent()) {
            RefreshTokenMapper.updateJpa(existing.get(), token);
            jpa.save(existing.get());
        } else {
            jpa.save(RefreshTokenMapper.toJpa(token));
        }
    }

    @Override
    public Optional<RefreshToken> findById(RefreshTokenId id) {
        return jpa.findById(id.value()).map(RefreshTokenMapper::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByHashedToken(String hashedToken) {
        return jpa.findByHashedToken(hashedToken).map(RefreshTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllForSubject(UUID subject) {
        jpa.revokeAllForSubject(subject);
    }
}
```

- [ ] **Step 10.7: Run integration test (just to compile + check JPA mappings against the migration)**

Run: `./mvnw test -Dtest=SmokeIntegrationTest`
Expected: PASS — Hibernate validates JPA entities against schema (V3 columns).

- [ ] **Step 10.8: Commit**

```bash
git add src/main/java/com/cardapio/identity/infrastructure/persistence
git commit -m "feat(identity): add JPA persistence layer (entities, repos, mappers, adapters)"
```

---

## Task 11: RegisterCustomerUseCase

**Files:**
- Create: `src/main/java/com/cardapio/identity/application/command/RegisterCustomerCommand.java`
- Create: `src/main/java/com/cardapio/identity/application/usecase/RegisterCustomerUseCase.java`
- Test: `src/test/java/com/cardapio/identity/application/usecase/RegisterCustomerUseCaseTest.java`

- [ ] **Step 11.1: `RegisterCustomerCommand`**

```java
package com.cardapio.identity.application.command;

public record RegisterCustomerCommand(
    String name,
    String email,
    String phoneNumber,
    String rawPassword
) {}
```

- [ ] **Step 11.2: Test (unit, with mocks)**

File: `src/test/java/com/cardapio/identity/application/usecase/RegisterCustomerUseCaseTest.java`

```java
package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.RegisterCustomerCommand;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.RawPassword;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegisterCustomerUseCaseTest {

    private final CustomerRepository repo = mock(CustomerRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final RegisterCustomerUseCase useCase = new RegisterCustomerUseCase(repo, hasher);

    @Test
    void registersWhenEmailIsNew() {
        when(repo.existsByEmail(any())).thenReturn(false);
        when(hasher.hash(any())).thenReturn(new HashedPassword("$2a$12$x"));

        Result<CustomerId> result = useCase.execute(new RegisterCustomerCommand(
            "Maria", "maria@example.com", "+5511912345678", "S3curePass!"));

        assertThat(result.isSuccess()).isTrue();
        verify(repo).save(any(Customer.class));
    }

    @Test
    void rejectsDuplicateEmail() {
        when(repo.existsByEmail(Email.of("dup@example.com"))).thenReturn(true);

        Result<CustomerId> result = useCase.execute(new RegisterCustomerCommand(
            "X", "dup@example.com", "+5511912345678", "S3curePass!"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<CustomerId>) result).notification().errors())
            .extracting("code").contains("EMAIL_ALREADY_REGISTERED");
        verify(repo, never()).save(any());
    }

    @Test
    void rejectsInvalidEmail() {
        Result<CustomerId> result = useCase.execute(new RegisterCustomerCommand(
            "X", "not-an-email", "+5511912345678", "S3curePass!"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<CustomerId>) result).notification().errors())
            .extracting("code").contains("INVALID_EMAIL");
    }

    @Test
    void rejectsWeakPassword() {
        Result<CustomerId> result = useCase.execute(new RegisterCustomerCommand(
            "X", "x@y.com", "+5511912345678", "weak"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<CustomerId>) result).notification().errors())
            .extracting("code").contains("WEAK_PASSWORD");
    }
}
```

- [ ] **Step 11.3: `RegisterCustomerUseCase`**

File: `src/main/java/com/cardapio/identity/application/usecase/RegisterCustomerUseCase.java`

```java
package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.RegisterCustomerCommand;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.RawPassword;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterCustomerUseCase {

    private final CustomerRepository repo;
    private final PasswordHasher hasher;

    public RegisterCustomerUseCase(CustomerRepository repo, PasswordHasher hasher) {
        this.repo = repo;
        this.hasher = hasher;
    }

    @Transactional
    public Result<CustomerId> execute(RegisterCustomerCommand cmd) {
        Notification n = Notification.empty();

        Email email = parseEmail(cmd.email(), n);
        PhoneNumber phone = parsePhone(cmd.phoneNumber(), n);
        RawPassword password = parsePassword(cmd.rawPassword(), n);

        if (cmd.name() == null || cmd.name().isBlank()) {
            n.addError("name", "BLANK_NAME", "nome obrigatório");
        }

        if (n.hasErrors()) return Result.failure(n);

        if (repo.existsByEmail(email)) {
            n.addError("email", "EMAIL_ALREADY_REGISTERED", "este e-mail já está cadastrado");
            return Result.failure(n);
        }

        HashedPassword hashed = hasher.hash(password);
        Customer customer = Customer.register(cmd.name(), email, phone, hashed);
        repo.save(customer);
        return Result.success(customer.id());
    }

    private Email parseEmail(String raw, Notification n) {
        try { return Email.of(raw); }
        catch (RuntimeException e) { n.addError("email", "INVALID_EMAIL", "e-mail inválido"); return null; }
    }

    private PhoneNumber parsePhone(String raw, Notification n) {
        try { return PhoneNumber.of(raw); }
        catch (RuntimeException e) { n.addError("phoneNumber", "INVALID_PHONE", "telefone inválido"); return null; }
    }

    private RawPassword parsePassword(String raw, Notification n) {
        try { return RawPassword.of(raw); }
        catch (RuntimeException e) { n.addError("password", "WEAK_PASSWORD", "senha fraca"); return null; }
    }
}
```

- [ ] **Step 11.4: Run, pass, commit.**

```bash
./mvnw test -Dtest=RegisterCustomerUseCaseTest
git add src/main/java/com/cardapio/identity/application src/test/java/com/cardapio/identity/application
git commit -m "feat(identity): add RegisterCustomerUseCase with Notification + Result"
```

---

## Task 12: LoginCustomerUseCase + RefreshTokenUseCase

**Files:**
- Create: `src/main/java/com/cardapio/identity/application/command/LoginCommand.java`
- Create: `src/main/java/com/cardapio/identity/application/command/RefreshTokenCommand.java`
- Create: `src/main/java/com/cardapio/identity/domain/exception/InvalidCredentialsException.java` (used by API layer to translate to 401)
- Create: `src/main/java/com/cardapio/identity/application/usecase/LoginCustomerUseCase.java`
- Create: `src/main/java/com/cardapio/identity/application/usecase/RefreshTokenUseCase.java`
- Test: `src/test/java/com/cardapio/identity/application/usecase/LoginCustomerUseCaseTest.java`
- Test: `src/test/java/com/cardapio/identity/application/usecase/RefreshTokenUseCaseTest.java`

- [ ] **Step 12.1: Commands and exception**

File: `src/main/java/com/cardapio/identity/application/command/LoginCommand.java`

```java
package com.cardapio.identity.application.command;

public record LoginCommand(String email, String rawPassword) {}
```

File: `src/main/java/com/cardapio/identity/application/command/RefreshTokenCommand.java`

```java
package com.cardapio.identity.application.command;

public record RefreshTokenCommand(String refreshToken) {}
```

File: `src/main/java/com/cardapio/identity/domain/exception/InvalidCredentialsException.java`

```java
package com.cardapio.identity.domain.exception;

import com.cardapio.shared.domain.DomainException;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "credenciais inválidas");
    }
}
```

- [ ] **Step 12.2: `LoginCustomerUseCaseTest`**

```java
package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.domain.model.*;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoginCustomerUseCaseTest {

    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final JwtIssuer issuer = mock(JwtIssuer.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-04T10:00:00Z"), ZoneId.of("UTC"));
    private final LoginCustomerUseCase useCase = new LoginCustomerUseCase(customers, refreshTokens, hasher, issuer, clock);

    @Test
    void issuesTokenPairOnValidCredentials() {
        Customer c = Customer.rehydrate(
            CustomerId.newId(), "Maria", Email.of("maria@example.com"),
            PhoneNumber.of("+5511912345678"), new HashedPassword("$2a$12$x"));
        when(customers.findByEmail(Email.of("maria@example.com"))).thenReturn(Optional.of(c));
        when(hasher.matches(any(), any())).thenReturn(true);
        Instant now = clock.instant();
        when(issuer.issueAccessToken(any(), any(), any())).thenReturn(new JwtIssuer.IssuedJwt("access.jwt", now.plus(Duration.ofMinutes(15))));
        when(issuer.generateOpaqueRefreshToken()).thenReturn("refresh-token-raw");
        when(issuer.refreshTokenExpiry(any())).thenReturn(now.plus(Duration.ofDays(30)));

        Result<TokenPair> result = useCase.execute(new LoginCommand("maria@example.com", "S3curePass!"));

        assertThat(result.isSuccess()).isTrue();
        TokenPair pair = result.getOrThrow();
        assertThat(pair.accessToken()).isEqualTo("access.jwt");
        assertThat(pair.refreshToken()).isEqualTo("refresh-token-raw");
        verify(refreshTokens).save(any());
    }

    @Test
    void rejectsUnknownEmail() {
        when(customers.findByEmail(any())).thenReturn(Optional.empty());

        Result<TokenPair> result = useCase.execute(new LoginCommand("nope@example.com", "anything!1"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<TokenPair>) result).notification().errors())
            .extracting("code").contains("INVALID_CREDENTIALS");
    }

    @Test
    void rejectsWrongPassword() {
        Customer c = Customer.rehydrate(
            CustomerId.newId(), "X", Email.of("x@y.com"),
            PhoneNumber.of("+5511912345678"), new HashedPassword("$2a$12$x"));
        when(customers.findByEmail(any())).thenReturn(Optional.of(c));
        when(hasher.matches(any(), any())).thenReturn(false);

        Result<TokenPair> result = useCase.execute(new LoginCommand("x@y.com", "WrongPass1!"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<TokenPair>) result).notification().errors())
            .extracting("code").contains("INVALID_CREDENTIALS");
    }
}
```

- [ ] **Step 12.3: `LoginCustomerUseCase`**

File: `src/main/java/com/cardapio/identity/application/usecase/LoginCustomerUseCase.java`

```java
package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.RawPassword;
import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

@Service
public class LoginCustomerUseCase {

    private final CustomerRepository customers;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher hasher;
    private final JwtIssuer issuer;
    private final Clock clock;

    public LoginCustomerUseCase(CustomerRepository customers, RefreshTokenRepository refreshTokens,
                                PasswordHasher hasher, JwtIssuer issuer, Clock clock) {
        this.customers = customers; this.refreshTokens = refreshTokens;
        this.hasher = hasher; this.issuer = issuer; this.clock = clock;
    }

    @Transactional
    public Result<TokenPair> execute(LoginCommand cmd) {
        Notification n = Notification.empty();
        Email email;
        try { email = Email.of(cmd.email()); }
        catch (RuntimeException e) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        Optional<Customer> maybeCustomer = customers.findByEmail(email);
        if (maybeCustomer.isEmpty()) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        Customer customer = maybeCustomer.get();
        RawPassword raw;
        try { raw = RawPassword.of(cmd.rawPassword()); }
        catch (RuntimeException e) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        if (!hasher.matches(raw, customer.passwordHash())) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        Instant now = clock.instant();
        var access = issuer.issueAccessToken(customer.id().value(), Audience.CUSTOMER, Set.of());
        String rawRefresh = issuer.generateOpaqueRefreshToken();
        Instant refreshExp = issuer.refreshTokenExpiry(now);
        Duration refreshTtl = Duration.between(now, refreshExp);

        RefreshToken token = RefreshToken.issue(
            customer.id().value(), Audience.CUSTOMER,
            sha256Hex(rawRefresh), now, refreshTtl);
        refreshTokens.save(token);

        return Result.success(new TokenPair(access.token(), access.expiresAt(), rawRefresh, refreshExp));
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
```

- [ ] **Step 12.4: `RefreshTokenUseCaseTest`**

```java
package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.RefreshTokenCommand;
import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenUseCaseTest {

    private final RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
    private final JwtIssuer issuer = mock(JwtIssuer.class);
    private final Instant now = Instant.parse("2026-05-04T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneId.of("UTC"));
    private final RefreshTokenUseCase useCase = new RefreshTokenUseCase(repo, issuer, clock);

    @Test
    void rotatesValidToken() {
        UUID subject = UUID.randomUUID();
        RefreshToken stored = RefreshToken.issue(subject, Audience.CUSTOMER,
            LoginCustomerUseCase.sha256Hex("raw-refresh"), now.minus(Duration.ofMinutes(5)), Duration.ofDays(30));
        when(repo.findByHashedToken(LoginCustomerUseCase.sha256Hex("raw-refresh"))).thenReturn(Optional.of(stored));
        when(issuer.issueAccessToken(any(), any(), any())).thenReturn(new JwtIssuer.IssuedJwt("new-access", now.plus(Duration.ofMinutes(15))));
        when(issuer.generateOpaqueRefreshToken()).thenReturn("new-refresh");
        when(issuer.refreshTokenExpiry(any())).thenReturn(now.plus(Duration.ofDays(30)));

        Result<TokenPair> result = useCase.execute(new RefreshTokenCommand("raw-refresh"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(stored.isRevoked()).isTrue();  // old token revoked after rotation
        verify(repo, times(2)).save(any());  // save revoked old + save new
    }

    @Test
    void rejectsExpiredToken() {
        UUID subject = UUID.randomUUID();
        RefreshToken expired = RefreshToken.issue(subject, Audience.CUSTOMER,
            LoginCustomerUseCase.sha256Hex("raw"), now.minus(Duration.ofDays(40)), Duration.ofDays(30));
        when(repo.findByHashedToken(any())).thenReturn(Optional.of(expired));

        Result<TokenPair> result = useCase.execute(new RefreshTokenCommand("raw"));

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void rejectsUnknownToken() {
        when(repo.findByHashedToken(any())).thenReturn(Optional.empty());
        Result<TokenPair> result = useCase.execute(new RefreshTokenCommand("garbage"));
        assertThat(result.isSuccess()).isFalse();
    }
}
```

- [ ] **Step 12.5: `RefreshTokenUseCase`**

```java
package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.RefreshTokenCommand;
import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Service
public class RefreshTokenUseCase {

    private final RefreshTokenRepository repo;
    private final JwtIssuer issuer;
    private final Clock clock;

    public RefreshTokenUseCase(RefreshTokenRepository repo, JwtIssuer issuer, Clock clock) {
        this.repo = repo; this.issuer = issuer; this.clock = clock;
    }

    @Transactional
    public Result<TokenPair> execute(RefreshTokenCommand cmd) {
        Notification n = Notification.empty();
        Instant now = clock.instant();
        String hashed = LoginCustomerUseCase.sha256Hex(cmd.refreshToken());

        Optional<RefreshToken> maybe = repo.findByHashedToken(hashed);
        if (maybe.isEmpty()) {
            n.addError("INVALID_REFRESH_TOKEN", "refresh token inválido");
            return Result.failure(n);
        }
        RefreshToken old = maybe.get();
        if (!old.isActiveAt(now)) {
            n.addError("INVALID_REFRESH_TOKEN", "refresh token expirado ou revogado");
            return Result.failure(n);
        }

        // rotate: revoke old, issue new
        old.revoke();
        repo.save(old);

        var access = issuer.issueAccessToken(old.subject(), old.audience(), Set.of());
        String rawNew = issuer.generateOpaqueRefreshToken();
        Instant newExp = issuer.refreshTokenExpiry(now);
        Duration ttl = Duration.between(now, newExp);
        RefreshToken next = RefreshToken.issue(old.subject(), old.audience(),
            LoginCustomerUseCase.sha256Hex(rawNew), now, ttl);
        repo.save(next);

        return Result.success(new TokenPair(access.token(), access.expiresAt(), rawNew, newExp));
    }
}
```

- [ ] **Step 12.6: Run, pass, commit.**

```bash
./mvnw test -Dtest=LoginCustomerUseCaseTest,RefreshTokenUseCaseTest
git add src/main/java/com/cardapio/identity/application src/main/java/com/cardapio/identity/domain/exception src/test/java/com/cardapio/identity/application
git commit -m "feat(identity): add LoginCustomerUseCase and RefreshTokenUseCase"
```

---

## Task 13: LoginAdminUseCase + DevAdminSeeder

**Files:**
- Create: `src/main/java/com/cardapio/identity/application/usecase/LoginAdminUseCase.java`
- Create: `src/main/java/com/cardapio/identity/infrastructure/seed/DevAdminSeeder.java`
- Test: `src/test/java/com/cardapio/identity/application/usecase/LoginAdminUseCaseTest.java`

- [ ] **Step 13.1: `LoginAdminUseCase`** (similar to LoginCustomerUseCase but uses `AdminRepository` and includes role claims)

```java
package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.domain.model.*;
import com.cardapio.identity.domain.port.AdminRepository;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class LoginAdminUseCase {

    private final AdminRepository admins;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher hasher;
    private final JwtIssuer issuer;
    private final Clock clock;

    public LoginAdminUseCase(AdminRepository admins, RefreshTokenRepository refreshTokens,
                             PasswordHasher hasher, JwtIssuer issuer, Clock clock) {
        this.admins = admins; this.refreshTokens = refreshTokens;
        this.hasher = hasher; this.issuer = issuer; this.clock = clock;
    }

    @Transactional
    public Result<TokenPair> execute(LoginCommand cmd) {
        Notification n = Notification.empty();
        Email email;
        try { email = Email.of(cmd.email()); }
        catch (RuntimeException e) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        Optional<Admin> maybe = admins.findByEmail(email);
        if (maybe.isEmpty()) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }
        Admin admin = maybe.get();

        RawPassword raw;
        try { raw = RawPassword.of(cmd.rawPassword()); }
        catch (RuntimeException e) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        if (!hasher.matches(raw, admin.passwordHash())) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        Instant now = clock.instant();
        var access = issuer.issueAccessToken(admin.id().value(), Audience.ADMIN, admin.roles());
        String rawRefresh = issuer.generateOpaqueRefreshToken();
        Instant refreshExp = issuer.refreshTokenExpiry(now);
        Duration ttl = Duration.between(now, refreshExp);

        RefreshToken token = RefreshToken.issue(admin.id().value(), Audience.ADMIN,
            LoginCustomerUseCase.sha256Hex(rawRefresh), now, ttl);
        refreshTokens.save(token);

        return Result.success(new TokenPair(access.token(), access.expiresAt(), rawRefresh, refreshExp));
    }
}
```

- [ ] **Step 13.2: `LoginAdminUseCaseTest`** (3 tests mirroring LoginCustomerUseCaseTest but with admin + roles)

```java
package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.domain.model.*;
import com.cardapio.identity.domain.port.AdminRepository;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LoginAdminUseCaseTest {

    private final AdminRepository admins = mock(AdminRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final JwtIssuer issuer = mock(JwtIssuer.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-04T10:00:00Z"), ZoneId.of("UTC"));
    private final LoginAdminUseCase useCase = new LoginAdminUseCase(admins, refreshTokens, hasher, issuer, clock);

    @Test
    void issuesTokenWithAdminAudienceAndRoles() {
        Admin admin = Admin.rehydrate(AdminId.newId(), "Boss", Email.of("boss@cardapio.com"),
            new HashedPassword("$2a$12$x"), Set.of(Role.OWNER));
        when(admins.findByEmail(any())).thenReturn(Optional.of(admin));
        when(hasher.matches(any(), any())).thenReturn(true);
        when(issuer.issueAccessToken(any(), eq(Audience.ADMIN), eq(Set.of(Role.OWNER))))
            .thenReturn(new JwtIssuer.IssuedJwt("admin.jwt", clock.instant().plus(Duration.ofMinutes(15))));
        when(issuer.generateOpaqueRefreshToken()).thenReturn("rfsh");
        when(issuer.refreshTokenExpiry(any())).thenReturn(clock.instant().plus(Duration.ofDays(30)));

        Result<TokenPair> result = useCase.execute(new LoginCommand("boss@cardapio.com", "S3curePass!"));

        assertThat(result.isSuccess()).isTrue();
        verify(issuer).issueAccessToken(any(), eq(Audience.ADMIN), eq(Set.of(Role.OWNER)));
    }

    @Test
    void rejectsUnknownAdmin() {
        when(admins.findByEmail(any())).thenReturn(Optional.empty());
        Result<TokenPair> result = useCase.execute(new LoginCommand("nope@x.com", "Anything!1"));
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void rejectsWrongPassword() {
        Admin admin = Admin.rehydrate(AdminId.newId(), "X", Email.of("x@y.com"),
            new HashedPassword("$2a$12$x"), Set.of(Role.MANAGER));
        when(admins.findByEmail(any())).thenReturn(Optional.of(admin));
        when(hasher.matches(any(), any())).thenReturn(false);
        Result<TokenPair> result = useCase.execute(new LoginCommand("x@y.com", "WrongPass1!"));
        assertThat(result.isSuccess()).isFalse();
    }
}
```

- [ ] **Step 13.3: `DevAdminSeeder`** (creates default OWNER admin on dev startup if none exists)

File: `src/main/java/com/cardapio/identity/infrastructure/seed/DevAdminSeeder.java`

```java
package com.cardapio.identity.infrastructure.seed;

import com.cardapio.identity.domain.model.Admin;
import com.cardapio.identity.domain.model.RawPassword;
import com.cardapio.identity.domain.model.Role;
import com.cardapio.identity.domain.port.AdminRepository;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.shared.domain.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("dev")
public class DevAdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(DevAdminSeeder.class);
    private static final Email DEFAULT_EMAIL = Email.of("admin@cardapio.local");
    private static final String DEFAULT_PASSWORD = "Admin@123!";

    private final AdminRepository admins;
    private final PasswordHasher hasher;

    public DevAdminSeeder(AdminRepository admins, PasswordHasher hasher) {
        this.admins = admins;
        this.hasher = hasher;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (admins.findByEmail(DEFAULT_EMAIL).isPresent()) return;
        Admin admin = Admin.create("Admin Dev", DEFAULT_EMAIL,
            hasher.hash(RawPassword.of(DEFAULT_PASSWORD)), Set.of(Role.OWNER));
        admins.save(admin);
        log.warn("DEV admin seeded: email={} password={}  (only in dev profile)", DEFAULT_EMAIL.value(), DEFAULT_PASSWORD);
    }
}
```

- [ ] **Step 13.4: Run, pass, commit.**

```bash
./mvnw test -Dtest=LoginAdminUseCaseTest
git add src/main/java/com/cardapio/identity/application/usecase/LoginAdminUseCase.java \
        src/main/java/com/cardapio/identity/infrastructure/seed/DevAdminSeeder.java \
        src/test/java/com/cardapio/identity/application/usecase/LoginAdminUseCaseTest.java
git commit -m "feat(identity): add LoginAdminUseCase and dev admin seeder"
```

---

## Task 14: GetMyProfile + UpdateMyProfile use cases

**Files:**
- Create: `src/main/java/com/cardapio/identity/application/dto/CustomerProfile.java`
- Create: `src/main/java/com/cardapio/identity/application/usecase/GetMyProfileUseCase.java`
- Create: `src/main/java/com/cardapio/identity/application/command/UpdateProfileCommand.java`
- Create: `src/main/java/com/cardapio/identity/application/usecase/UpdateMyProfileUseCase.java`
- Test: `src/test/java/com/cardapio/identity/application/usecase/MyProfileUseCasesTest.java`

- [ ] **Step 14.1: DTO + commands**

File: `src/main/java/com/cardapio/identity/application/dto/CustomerProfile.java`

```java
package com.cardapio.identity.application.dto;

import com.cardapio.identity.domain.model.CustomerId;

public record CustomerProfile(CustomerId id, String name, String email, String phoneNumber) {}
```

File: `src/main/java/com/cardapio/identity/application/command/UpdateProfileCommand.java`

```java
package com.cardapio.identity.application.command;

import com.cardapio.identity.domain.model.CustomerId;

public record UpdateProfileCommand(CustomerId customerId, String name, String phoneNumber) {}
```

- [ ] **Step 14.2: Use cases**

File: `src/main/java/com/cardapio/identity/application/usecase/GetMyProfileUseCase.java`

```java
package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetMyProfileUseCase {
    private final CustomerRepository customers;
    public GetMyProfileUseCase(CustomerRepository customers) { this.customers = customers; }

    @Transactional(readOnly = true)
    public Result<CustomerProfile> execute(CustomerId id) {
        return customers.findById(id)
            .map(c -> Result.success(new CustomerProfile(c.id(), c.name(), c.email().value(), c.phoneNumber().value())))
            .orElseGet(() -> {
                Notification n = Notification.empty();
                n.addError("CUSTOMER_NOT_FOUND", "cliente não encontrado");
                return Result.failure(n);
            });
    }
}
```

File: `src/main/java/com/cardapio/identity/application/usecase/UpdateMyProfileUseCase.java`

```java
package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.UpdateProfileCommand;
import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UpdateMyProfileUseCase {
    private final CustomerRepository customers;
    public UpdateMyProfileUseCase(CustomerRepository customers) { this.customers = customers; }

    @Transactional
    public Result<CustomerProfile> execute(UpdateProfileCommand cmd) {
        Notification n = Notification.empty();

        if (cmd.name() == null || cmd.name().isBlank()) n.addError("name", "BLANK_NAME", "nome obrigatório");

        PhoneNumber phone = null;
        try { phone = PhoneNumber.of(cmd.phoneNumber()); }
        catch (RuntimeException e) { n.addError("phoneNumber", "INVALID_PHONE", "telefone inválido"); }

        Optional<Customer> maybe = customers.findById(cmd.customerId());
        if (maybe.isEmpty()) n.addError("CUSTOMER_NOT_FOUND", "cliente não encontrado");

        if (n.hasErrors()) return Result.failure(n);

        Customer c = maybe.get();
        c.updateProfile(cmd.name(), phone);
        customers.save(c);
        return Result.success(new CustomerProfile(c.id(), c.name(), c.email().value(), c.phoneNumber().value()));
    }
}
```

- [ ] **Step 14.3: Tests**

File: `src/test/java/com/cardapio/identity/application/usecase/MyProfileUseCasesTest.java`

```java
package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.UpdateProfileCommand;
import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MyProfileUseCasesTest {

    private final CustomerRepository repo = mock(CustomerRepository.class);

    @Test
    void getProfileReturnsDto() {
        CustomerId id = CustomerId.newId();
        Customer c = Customer.rehydrate(id, "Maria", Email.of("m@x.com"),
            PhoneNumber.of("+5511912345678"), new HashedPassword("$2a$12$x"));
        when(repo.findById(id)).thenReturn(Optional.of(c));

        Result<CustomerProfile> r = new GetMyProfileUseCase(repo).execute(id);
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getOrThrow().email()).isEqualTo("m@x.com");
    }

    @Test
    void updateProfileSavesChanges() {
        CustomerId id = CustomerId.newId();
        Customer c = Customer.rehydrate(id, "Maria", Email.of("m@x.com"),
            PhoneNumber.of("+5511912345678"), new HashedPassword("$2a$12$x"));
        when(repo.findById(id)).thenReturn(Optional.of(c));

        var useCase = new UpdateMyProfileUseCase(repo);
        Result<CustomerProfile> r = useCase.execute(new UpdateProfileCommand(id, "Maria Nova", "+5511987654321"));

        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getOrThrow().name()).isEqualTo("Maria Nova");
        verify(repo).save(any(Customer.class));
    }

    @Test
    void updateRejectsInvalidPhone() {
        when(repo.findById(any())).thenReturn(Optional.of(Customer.rehydrate(
            CustomerId.newId(), "X", Email.of("x@y.com"),
            PhoneNumber.of("+5511912345678"), new HashedPassword("$2a$12$x"))));

        Result<CustomerProfile> r = new UpdateMyProfileUseCase(repo)
            .execute(new UpdateProfileCommand(CustomerId.newId(), "X", "abc"));
        assertThat(r.isSuccess()).isFalse();
    }
}
```

- [ ] **Step 14.4: Run, pass, commit.**

```bash
./mvnw test -Dtest=MyProfileUseCasesTest
git add src/main/java/com/cardapio/identity/application src/test/java/com/cardapio/identity/application/usecase/MyProfileUseCasesTest.java
git commit -m "feat(identity): add GetMyProfile and UpdateMyProfile use cases"
```

---

## Task 15: Spring Security config + JWT authentication filter

**Files:**
- Create: `src/main/java/com/cardapio/identity/api/security/CardapioPrincipal.java`
- Create: `src/main/java/com/cardapio/identity/api/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/cardapio/identity/api/security/SecurityConfig.java`

- [ ] **Step 15.1: `CardapioPrincipal`** (custom principal exposed in `Authentication.getPrincipal()`)

```java
package com.cardapio.identity.api.security;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Role;

import java.util.Set;
import java.util.UUID;

public record CardapioPrincipal(UUID subject, Audience audience, Set<Role> roles) {}
```

- [ ] **Step 15.2: `JwtAuthenticationFilter`**

```java
package com.cardapio.identity.api.security;

import com.cardapio.identity.domain.port.JwtVerifier;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtVerifier verifier;

    public JwtAuthenticationFilter(JwtVerifier verifier) { this.verifier = verifier; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                JwtVerifier.VerifiedJwt verified = verifier.verify(token);
                CardapioPrincipal principal = new CardapioPrincipal(
                    verified.subject(), verified.audience(), verified.roles());
                List<SimpleGrantedAuthority> authorities = verified.roles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                    .toList();
                var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException ignored) {
                // invalid token → proceed unauthenticated; Spring Security will reject if endpoint needs auth
            }
        }
        chain.doFilter(req, res);
    }
}
```

- [ ] **Step 15.3: `SecurityConfig`**

```java
package com.cardapio.identity.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register",
                                                    "/api/v1/auth/login",
                                                    "/api/v1/auth/refresh",
                                                    "/api/v1/admin/auth/login").permitAll()
                .requestMatchers("/api/v1/admin/**").hasAnyRole("OWNER", "MANAGER", "OPERATOR")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

- [ ] **Step 15.4: Verify HealthSmokeTest still passes (was failing after Task 1 added security)**

Run: `./mvnw test -Dtest=HealthSmokeTest`
Expected: PASS — `/actuator/health` is permitAll-ed.

- [ ] **Step 15.5: Commit**

```bash
git add src/main/java/com/cardapio/identity/api/security
git commit -m "feat(identity): add Spring Security config with JWT filter and method security"
```

---

## Task 16: REST controllers (auth + me)

**Files:**
- Create: `src/main/java/com/cardapio/identity/api/dto/RegisterRequest.java`
- Create: `src/main/java/com/cardapio/identity/api/dto/LoginRequest.java`
- Create: `src/main/java/com/cardapio/identity/api/dto/RefreshRequest.java`
- Create: `src/main/java/com/cardapio/identity/api/dto/TokenPairResponse.java`
- Create: `src/main/java/com/cardapio/identity/api/dto/ProfileResponse.java`
- Create: `src/main/java/com/cardapio/identity/api/dto/UpdateProfileRequest.java`
- Create: `src/main/java/com/cardapio/identity/api/rest/CustomerAuthController.java`
- Create: `src/main/java/com/cardapio/identity/api/rest/AdminAuthController.java`
- Create: `src/main/java/com/cardapio/identity/api/rest/MeController.java`

- [ ] **Step 16.1: DTOs**

File: `src/main/java/com/cardapio/identity/api/dto/RegisterRequest.java`

```java
package com.cardapio.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank String name,
    @NotBlank String email,
    @NotBlank String phoneNumber,
    @NotBlank String password
) {}
```

File: `src/main/java/com/cardapio/identity/api/dto/LoginRequest.java`

```java
package com.cardapio.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
```

File: `src/main/java/com/cardapio/identity/api/dto/RefreshRequest.java`

```java
package com.cardapio.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {}
```

File: `src/main/java/com/cardapio/identity/api/dto/TokenPairResponse.java`

```java
package com.cardapio.identity.api.dto;

import com.cardapio.identity.domain.model.TokenPair;

import java.time.Instant;

public record TokenPairResponse(
    String accessToken,
    Instant accessTokenExpiresAt,
    String refreshToken,
    Instant refreshTokenExpiresAt
) {
    public static TokenPairResponse from(TokenPair t) {
        return new TokenPairResponse(t.accessToken(), t.accessTokenExpiresAt(),
            t.refreshToken(), t.refreshTokenExpiresAt());
    }
}
```

File: `src/main/java/com/cardapio/identity/api/dto/ProfileResponse.java`

```java
package com.cardapio.identity.api.dto;

import com.cardapio.identity.application.dto.CustomerProfile;

import java.util.UUID;

public record ProfileResponse(UUID id, String name, String email, String phoneNumber) {
    public static ProfileResponse from(CustomerProfile p) {
        return new ProfileResponse(p.id().value(), p.name(), p.email(), p.phoneNumber());
    }
}
```

File: `src/main/java/com/cardapio/identity/api/dto/UpdateProfileRequest.java`

```java
package com.cardapio.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(@NotBlank String name, @NotBlank String phoneNumber) {}
```

- [ ] **Step 16.2: `CustomerAuthController`**

```java
package com.cardapio.identity.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.identity.api.dto.*;
import com.cardapio.identity.application.command.*;
import com.cardapio.identity.application.usecase.LoginCustomerUseCase;
import com.cardapio.identity.application.usecase.RefreshTokenUseCase;
import com.cardapio.identity.application.usecase.RegisterCustomerUseCase;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.shared.domain.Result;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class CustomerAuthController {

    private final RegisterCustomerUseCase register;
    private final LoginCustomerUseCase login;
    private final RefreshTokenUseCase refresh;

    public CustomerAuthController(RegisterCustomerUseCase register, LoginCustomerUseCase login, RefreshTokenUseCase refresh) {
        this.register = register; this.login = login; this.refresh = refresh;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        Result<CustomerId> r = register.execute(new RegisterCustomerCommand(req.name(), req.email(), req.phoneNumber(), req.password()));
        return switch (r) {
            case Result.Success<CustomerId> s -> ResponseEntity.created(URI.create("/api/v1/me"))
                .body(Map.of("id", s.value().value()));
            case Result.Failure<CustomerId> f -> unprocessable(f);
        };
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        Result<TokenPair> r = login.execute(new LoginCommand(req.email(), req.password()));
        return switch (r) {
            case Result.Success<TokenPair> s -> ResponseEntity.ok(TokenPairResponse.from(s.value()));
            case Result.Failure<TokenPair> f -> ResponseEntity.status(401)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetails.fromNotification(f.notification()));
        };
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest req) {
        Result<TokenPair> r = refresh.execute(new RefreshTokenCommand(req.refreshToken()));
        return switch (r) {
            case Result.Success<TokenPair> s -> ResponseEntity.ok(TokenPairResponse.from(s.value()));
            case Result.Failure<TokenPair> f -> ResponseEntity.status(401)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetails.fromNotification(f.notification()));
        };
    }

    private ResponseEntity<ProblemDetail> unprocessable(Result.Failure<?> f) {
        return ResponseEntity.unprocessableEntity()
            .contentType(MediaType.parseMediaType("application/problem+json"))
            .body(ProblemDetails.fromNotification(f.notification()));
    }
}
```

- [ ] **Step 16.3: `AdminAuthController`**

```java
package com.cardapio.identity.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.identity.api.dto.LoginRequest;
import com.cardapio.identity.api.dto.TokenPairResponse;
import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.application.usecase.LoginAdminUseCase;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.shared.domain.Result;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final LoginAdminUseCase login;

    public AdminAuthController(LoginAdminUseCase login) { this.login = login; }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        Result<TokenPair> r = login.execute(new LoginCommand(req.email(), req.password()));
        return switch (r) {
            case Result.Success<TokenPair> s -> ResponseEntity.ok(TokenPairResponse.from(s.value()));
            case Result.Failure<TokenPair> f -> ResponseEntity.status(401)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetails.fromNotification(f.notification()));
        };
    }
}
```

- [ ] **Step 16.4: `MeController`** (uses `@AuthenticationPrincipal` to get the JWT subject)

```java
package com.cardapio.identity.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.identity.api.dto.ProfileResponse;
import com.cardapio.identity.api.dto.UpdateProfileRequest;
import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.identity.application.command.UpdateProfileCommand;
import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.application.usecase.GetMyProfileUseCase;
import com.cardapio.identity.application.usecase.UpdateMyProfileUseCase;
import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.shared.domain.Result;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final GetMyProfileUseCase getMy;
    private final UpdateMyProfileUseCase updateMy;

    public MeController(GetMyProfileUseCase getMy, UpdateMyProfileUseCase updateMy) {
        this.getMy = getMy; this.updateMy = updateMy;
    }

    @GetMapping
    public ResponseEntity<?> me(@AuthenticationPrincipal CardapioPrincipal principal) {
        if (principal == null || principal.audience() != Audience.CUSTOMER) {
            return ResponseEntity.status(403).build();
        }
        Result<CustomerProfile> r = getMy.execute(CustomerId.of(principal.subject()));
        return switch (r) {
            case Result.Success<CustomerProfile> s -> ResponseEntity.ok(ProfileResponse.from(s.value()));
            case Result.Failure<CustomerProfile> f -> ResponseEntity.status(404)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetails.fromNotification(f.notification()));
        };
    }

    @PutMapping
    public ResponseEntity<?> update(@AuthenticationPrincipal CardapioPrincipal principal,
                                    @Valid @RequestBody UpdateProfileRequest req) {
        if (principal == null || principal.audience() != Audience.CUSTOMER) {
            return ResponseEntity.status(403).build();
        }
        Result<CustomerProfile> r = updateMy.execute(new UpdateProfileCommand(
            CustomerId.of(principal.subject()), req.name(), req.phoneNumber()));
        return switch (r) {
            case Result.Success<CustomerProfile> s -> ResponseEntity.ok(ProfileResponse.from(s.value()));
            case Result.Failure<CustomerProfile> f -> ResponseEntity.unprocessableEntity()
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetails.fromNotification(f.notification()));
        };
    }
}
```

- [ ] **Step 16.5: Run all tests to verify nothing broken**

Run: `./mvnw test`
Expected: ALL pass.

- [ ] **Step 16.6: Commit**

```bash
git add src/main/java/com/cardapio/identity/api
git commit -m "feat(identity): add customer/admin auth controllers and /me endpoints"
```

---

## Task 17: End-to-end integration test

**Files:**
- Create: `src/test/java/com/cardapio/identity/api/IdentityE2ETest.java`

- [ ] **Step 17.1: E2E test covering register → login → /me → refresh**

File: `src/test/java/com/cardapio/identity/api/IdentityE2ETest.java`

```java
package com.cardapio.identity.api;

import com.cardapio.support.PostgresTestContainerConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class IdentityE2ETest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void fullRegisterLoginRefreshFlow() throws Exception {
        // 1. Register
        String registerBody = """
            {
              "name": "Maria Silva",
              "email": "maria-e2e@example.com",
              "phoneNumber": "+5511912345678",
              "password": "S3curePass!"
            }
            """;
        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(registerBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());

        // 2. Login
        String loginBody = """
            { "email": "maria-e2e@example.com", "password": "S3curePass!" }
            """;
        MvcResult loginResult = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(loginBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andReturn();

        JsonNode loginJson = json.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("accessToken").asText();
        String refreshToken = loginJson.get("refreshToken").asText();

        // 3. Use access token on /me
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("maria-e2e@example.com"))
            .andExpect(jsonPath("$.name").value("Maria Silva"));

        // 4. Refresh token
        String refreshBody = "{ \"refreshToken\": \"" + refreshToken + "\" }";
        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON).content(refreshBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists());

        // 5. Old refresh should be revoked now (rotation)
        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON).content(refreshBody))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsLoginWithWrongPassword() throws Exception {
        // pre-register a user
        String reg = """
            {"name":"X","email":"wrongpass@example.com","phoneNumber":"+5511912345678","password":"S3curePass!"}
            """;
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(reg))
            .andExpect(status().isCreated());

        String wrong = """
            {"email":"wrongpass@example.com","password":"WrongPass!1"}
            """;
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(wrong))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void meRequiresAuth() throws Exception {
        mvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 17.2: Run, pass, commit.**

Run: `./mvnw test -Dtest=IdentityE2ETest`
Expected: 3 tests passing — full flow + wrong password + unauthenticated /me.

```bash
git add src/test/java/com/cardapio/identity/api/IdentityE2ETest.java
git commit -m "test(identity): add end-to-end auth flow integration test"
```

---

## Task 18: Final verification + tag

- [ ] **Step 18.1: Clean build with all tests**

Run: `./mvnw clean verify`
Expected: BUILD SUCCESS — all tests across Phase 1.A + 1.B passing.

- [ ] **Step 18.2: Live smoke test on dev profile**

Pre-condition: `docker compose up -d`
Run: `./mvnw spring-boot:run` in background; wait ~30s; then:

```bash
# Check health
curl http://localhost:8080/actuator/health
# Should be 200, status=UP

# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Maria","email":"maria@test.com","phoneNumber":"+5511912345678","password":"S3curePass!"}'
# Expected 201 with id

# Login admin (DevAdminSeeder created admin@cardapio.local / Admin@123!)
curl -X POST http://localhost:8080/api/v1/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@cardapio.local","password":"Admin@123!"}'
# Expected 200 with tokens
```

Stop the app with Ctrl+C / kill.

- [ ] **Step 18.3: Run Modulith verifier and ArchUnit (sanity)**

```bash
./mvnw test -Dtest=ModulithVerificationTest,CleanArchitectureTest
```
Expected: PASS. The new `identity` module's `@ApplicationModule(allowedDependencies = "shared")` + sub-package structure passes both checks.

- [ ] **Step 18.4: Tag the milestone**

```bash
git tag -a phase-1b-identity -m "Phase 1.B complete: Customer + Admin identity, JWT, refresh, /me"
```

---

## Self-Review Checklist

- [ ] All 18 tasks committed (~18-20 commits in addition to Phase 1.A)
- [ ] `./mvnw clean verify` exits 0
- [ ] No `org.springframework` imports under `com.cardapio.identity.domain` (CleanArchitectureTest enforces)
- [ ] Modulith verifier passes — `identity` module declares `allowedDependencies = "shared"` and respects it
- [ ] All 4 profiles (dev/test/staging/prod) have JWT secret config (env var in non-dev)
- [ ] `IdentityE2ETest` covers full register → login → /me → refresh + revocation
- [ ] Dev seeder creates default admin only when none exists, only on `dev` profile
- [ ] BCrypt hasher uses cost 12

---

## Definition of Done — Phase 1.B

- ✅ `identity` Modulith module created with `allowedDependencies = "shared"`
- ✅ Customer aggregate with registration event
- ✅ Admin aggregate with role-based access
- ✅ RefreshToken aggregate with rotation + revocation
- ✅ Domain ports for all repos + PasswordHasher + JwtIssuer + JwtVerifier
- ✅ JPA persistence layer with proper domain↔persistence mapping
- ✅ Use cases returning `Result<T>` with `Notification` for business errors
- ✅ Spring Security wired with custom JWT filter + `@PreAuthorize` ready for admin endpoints
- ✅ REST endpoints: register, login, refresh, admin/login, GET/PUT /me
- ✅ End-to-end integration test covers happy path + wrong password + unauthenticated
- ✅ DevAdminSeeder bootstraps a default admin in dev profile

**Next plan (to be written):** Phase 1.C — Catalog context (Category, Product with variations + addons + half-half, OperatingHours, public `/menu` + admin CRUD).
