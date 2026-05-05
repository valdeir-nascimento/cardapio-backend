# Phase 1.C — Catalog Context Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the `catalog` bounded context — Categories, Products (with variations P/M/G, add-on groups, meio-a-meio), OperatingHours; public `/menu` browsing endpoints + admin CRUD endpoints with role-based authorization.

**Architecture:** Clean Architecture inside `com.cardapio.catalog` module. Aggregates: `Category`, `Product` (root, contains `Variation` + `AddOnGroup` entities; `AddOnItem` is entity inside `AddOnGroup`), `OperatingHours`. JPA persistence with one-to-many cascading for product children. Application services return `Result<T>` for business outcomes; queries return read-only DTO views. Admin endpoints require role `OWNER` or `MANAGER`; public endpoints anonymous.

**Tech Stack:** Reuses everything from Phase 1.A/1.B. No new dependencies.

**Reference:**
- Spec: [docs/superpowers/specs/2026-05-04-cardapio-digital-backend-design.md](../specs/2026-05-04-cardapio-digital-backend-design.md) (Section 3 `catalog`, Section 4 public + admin endpoints)
- Phase 1.B plan: [docs/superpowers/plans/2026-05-04-phase-1b-identity.md](./2026-05-04-phase-1b-identity.md) — patterns for JPA layer, use cases, controllers

**Out of scope (deferred):**
- Image upload to R2/S3 → admin sends `imageUrl` as a string only; binary upload is Phase 5 / a later phase
- Cart consumption of catalog → Phase 2 (ordering)
- Stock reservation/decrement on order → Phase 2 (ordering events)
- `ProductOutOfStock` event publication → Phase 4 (notification)

---

## File Structure

```
src/main/java/com/cardapio/catalog/
├── domain/
│   ├── model/
│   │   ├── CategoryId.java
│   │   ├── Category.java
│   │   ├── ProductId.java
│   │   ├── Product.java
│   │   ├── VariationId.java
│   │   ├── Variation.java
│   │   ├── AddOnGroupId.java
│   │   ├── AddOnGroup.java
│   │   ├── AddOnItemId.java
│   │   ├── AddOnItem.java
│   │   ├── Stock.java                  (VO: nullable quantity or "untracked")
│   │   ├── OperatingHours.java         (aggregate: 7 days)
│   │   ├── DayHours.java               (entity: dayOfWeek + intervals)
│   │   └── TimeRange.java              (VO: openTime, closeTime, validates within day)
│   ├── port/
│   │   ├── CategoryRepository.java
│   │   ├── ProductRepository.java
│   │   └── OperatingHoursRepository.java
│   └── exception/
│       ├── CategoryNotFoundException.java
│       └── ProductNotFoundException.java
├── application/
│   ├── command/
│   │   ├── CreateCategoryCommand.java
│   │   ├── UpdateCategoryCommand.java
│   │   ├── CreateProductCommand.java   (rich: includes variations + addons)
│   │   ├── UpdateProductCommand.java
│   │   ├── SetProductAvailabilityCommand.java
│   │   ├── SetProductStockCommand.java
│   │   └── UpdateOperatingHoursCommand.java
│   ├── usecase/
│   │   ├── CreateCategoryUseCase.java
│   │   ├── UpdateCategoryUseCase.java
│   │   ├── DeleteCategoryUseCase.java
│   │   ├── CreateProductUseCase.java
│   │   ├── UpdateProductUseCase.java
│   │   ├── DeleteProductUseCase.java
│   │   ├── SetProductAvailabilityUseCase.java
│   │   ├── SetProductStockUseCase.java
│   │   ├── UpdateOperatingHoursUseCase.java
│   │   ├── GetMenuQuery.java           (query: returns MenuView)
│   │   ├── GetProductDetailsQuery.java
│   │   └── GetOperatingHoursQuery.java
│   └── dto/
│       ├── MenuView.java               (categories with active products)
│       ├── CategoryView.java
│       ├── ProductSummaryView.java     (used in menu listing)
│       ├── ProductDetailsView.java     (with variations + addons)
│       ├── VariationView.java
│       ├── AddOnGroupView.java
│       ├── AddOnItemView.java
│       └── OperatingHoursView.java
├── infrastructure/
│   └── persistence/
│       ├── jpa/                         (6 entities)
│       │   ├── CategoryJpaEntity.java
│       │   ├── ProductJpaEntity.java
│       │   ├── VariationJpaEntity.java
│       │   ├── AddOnGroupJpaEntity.java
│       │   ├── AddOnItemJpaEntity.java
│       │   └── OperatingHoursJpaEntity.java
│       ├── repository/                  (3 Spring Data repos)
│       │   ├── SpringCategoryJpaRepository.java
│       │   ├── SpringProductJpaRepository.java
│       │   └── SpringOperatingHoursJpaRepository.java
│       ├── mapper/                      (3 mappers)
│       │   ├── CategoryMapper.java
│       │   ├── ProductMapper.java       (handles variations + addons)
│       │   └── OperatingHoursMapper.java
│       └── adapter/                     (3 adapters)
│           ├── CategoryRepositoryAdapter.java
│           ├── ProductRepositoryAdapter.java
│           └── OperatingHoursRepositoryAdapter.java
├── api/
│   ├── rest/
│   │   ├── CategoryAdminController.java
│   │   ├── ProductAdminController.java
│   │   ├── OperatingHoursAdminController.java
│   │   ├── PublicMenuController.java
│   │   └── PublicOperatingHoursController.java
│   └── dto/                             (request/response records)
│       ├── CategoryRequest.java + CategoryResponse.java
│       ├── ProductRequest.java + ProductResponse.java
│       ├── VariationRequest.java + AddOnGroupRequest.java + AddOnItemRequest.java
│       ├── SetAvailabilityRequest.java + SetStockRequest.java
│       ├── OperatingHoursRequest.java + OperatingHoursResponse.java
│       └── MenuResponse.java + ProductDetailsResponse.java
└── package-info.java                    (@ApplicationModule)

src/main/resources/db/migration/
└── V4__catalog_tables.sql

src/main/java/com/cardapio/identity/api/security/SecurityConfig.java   (modify: add admin path matchers)
```

---

## Task 1: Catalog module skeleton

**Files:**
- Create: `src/main/java/com/cardapio/catalog/package-info.java`

- [ ] **Step 1.1:** Create the package-info

```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Catalog",
    allowedDependencies = {"shared"}
)
package com.cardapio.catalog;
```

- [ ] **Step 1.2:** Verify Modulith verifier still passes

Run: `./mvnw test -Dtest=ModulithVerificationTest`
Expected: PASS.

- [ ] **Step 1.3:** Commit

```bash
git add src/main/java/com/cardapio/catalog/package-info.java
git commit -m "chore(catalog): add module skeleton"
```

---

## Task 2: Catalog value objects (IDs + Stock + TimeRange)

**Files:**
- Create: 5 ID records under `src/main/java/com/cardapio/catalog/domain/model/`
- Create: `Stock.java`, `TimeRange.java`
- Test: `src/test/java/com/cardapio/catalog/domain/model/StockTest.java`
- Test: `src/test/java/com/cardapio/catalog/domain/model/TimeRangeTest.java`

- [ ] **Step 2.1:** Create the 5 ID records (CategoryId, ProductId, VariationId, AddOnGroupId, AddOnItemId)

File: `src/main/java/com/cardapio/catalog/domain/model/CategoryId.java`

```java
package com.cardapio.catalog.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CategoryId(UUID value) {
    public CategoryId { Objects.requireNonNull(value, "value"); }
    public static CategoryId newId() { return new CategoryId(UUID.randomUUID()); }
    public static CategoryId of(UUID value) { return new CategoryId(value); }
    public static CategoryId of(String value) { return new CategoryId(UUID.fromString(value)); }
}
```

Repeat the same pattern (changing only the type name) for: `ProductId`, `VariationId`, `AddOnGroupId`, `AddOnItemId`. All 5 files have identical structure modulo the class name.

- [ ] **Step 2.2:** Tests for `Stock` and `TimeRange`

File: `src/test/java/com/cardapio/catalog/domain/model/StockTest.java`

```java
package com.cardapio.catalog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    @Test
    void untrackedStockHasNoQuantity() {
        Stock stock = Stock.untracked();
        assertThat(stock.isTracked()).isFalse();
        assertThat(stock.isInStock()).isTrue();
    }

    @Test
    void trackedZeroIsOutOfStock() {
        Stock stock = Stock.of(0);
        assertThat(stock.isTracked()).isTrue();
        assertThat(stock.isInStock()).isFalse();
        assertThat(stock.quantity()).isEqualTo(0);
    }

    @Test
    void trackedPositiveIsInStock() {
        Stock stock = Stock.of(5);
        assertThat(stock.isTracked()).isTrue();
        assertThat(stock.isInStock()).isTrue();
        assertThat(stock.quantity()).isEqualTo(5);
    }

    @Test
    void rejectsNegativeQuantity() {
        assertThatThrownBy(() -> Stock.of(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decrementReducesQuantity() {
        Stock stock = Stock.of(10).decrement(3);
        assertThat(stock.quantity()).isEqualTo(7);
    }

    @Test
    void decrementBelowZeroIsForbidden() {
        assertThatThrownBy(() -> Stock.of(2).decrement(5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decrementUntrackedReturnsUntracked() {
        Stock untracked = Stock.untracked();
        assertThat(untracked.decrement(10).isTracked()).isFalse();
    }
}
```

File: `src/test/java/com/cardapio/catalog/domain/model/TimeRangeTest.java`

```java
package com.cardapio.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeRangeTest {

    @Test
    void acceptsValidRange() {
        TimeRange range = TimeRange.of(LocalTime.of(8, 0), LocalTime.of(18, 0));
        assertThat(range.openTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(range.closeTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void rejectsCloseBeforeOpen() {
        assertThatThrownBy(() -> TimeRange.of(LocalTime.of(18, 0), LocalTime.of(8, 0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("close");
    }

    @Test
    void rejectsEqualOpenAndClose() {
        assertThatThrownBy(() -> TimeRange.of(LocalTime.of(8, 0), LocalTime.of(8, 0)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void containsTimeWithinRange() {
        TimeRange range = TimeRange.of(LocalTime.of(8, 0), LocalTime.of(18, 0));
        assertThat(range.contains(LocalTime.of(12, 0))).isTrue();
        assertThat(range.contains(LocalTime.of(8, 0))).isTrue();   // inclusive open
        assertThat(range.contains(LocalTime.of(18, 0))).isFalse(); // exclusive close
        assertThat(range.contains(LocalTime.of(7, 59))).isFalse();
    }
}
```

- [ ] **Step 2.3:** Implement `Stock`

File: `src/main/java/com/cardapio/catalog/domain/model/Stock.java`

```java
package com.cardapio.catalog.domain.model;

public final class Stock {

    private final Integer quantity;  // null = untracked

    private Stock(Integer quantity) {
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("stock quantity must be non-negative");
        }
        this.quantity = quantity;
    }

    public static Stock untracked() { return new Stock(null); }
    public static Stock of(int quantity) { return new Stock(quantity); }

    public boolean isTracked() { return quantity != null; }
    public boolean isInStock() { return !isTracked() || quantity > 0; }
    public int quantity() {
        if (!isTracked()) throw new IllegalStateException("untracked stock has no quantity");
        return quantity;
    }
    public Integer rawQuantity() { return quantity; }  // null-safe accessor for persistence

    public Stock decrement(int amount) {
        if (!isTracked()) return this;
        if (amount < 0) throw new IllegalArgumentException("decrement must be non-negative");
        int next = quantity - amount;
        if (next < 0) throw new IllegalArgumentException("not enough stock");
        return new Stock(next);
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof Stock s)) return false;
        return java.util.Objects.equals(quantity, s.quantity);
    }
    @Override public int hashCode() { return java.util.Objects.hashCode(quantity); }
}
```

- [ ] **Step 2.4:** Implement `TimeRange`

File: `src/main/java/com/cardapio/catalog/domain/model/TimeRange.java`

```java
package com.cardapio.catalog.domain.model;

import java.time.LocalTime;
import java.util.Objects;

public record TimeRange(LocalTime openTime, LocalTime closeTime) {

    public TimeRange {
        Objects.requireNonNull(openTime, "openTime");
        Objects.requireNonNull(closeTime, "closeTime");
        if (!closeTime.isAfter(openTime)) {
            throw new IllegalArgumentException("close time must be after open time");
        }
    }

    public static TimeRange of(LocalTime open, LocalTime close) { return new TimeRange(open, close); }

    public boolean contains(LocalTime time) {
        return !time.isBefore(openTime) && time.isBefore(closeTime);
    }
}
```

- [ ] **Step 2.5:** Run tests, pass

Run: `./mvnw test -Dtest=StockTest,TimeRangeTest`
Expected: 11 tests pass.

- [ ] **Step 2.6:** Commit

```bash
git add src/main/java/com/cardapio/catalog/domain/model src/test/java/com/cardapio/catalog/domain/model
git commit -m "feat(catalog): add value objects (ids, Stock, TimeRange)"
```

---

## Task 3: Category aggregate

**Files:**
- Create: `src/main/java/com/cardapio/catalog/domain/model/Category.java`
- Create: `src/main/java/com/cardapio/catalog/domain/exception/CategoryNotFoundException.java`
- Test: `src/test/java/com/cardapio/catalog/domain/model/CategoryTest.java`

- [ ] **Step 3.1:** Test

File: `src/test/java/com/cardapio/catalog/domain/model/CategoryTest.java`

```java
package com.cardapio.catalog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Test
    void createsActiveCategory() {
        Category category = Category.create("Pizzas", 1);
        assertThat(category.id()).isNotNull();
        assertThat(category.name()).isEqualTo("Pizzas");
        assertThat(category.displayOrder()).isEqualTo(1);
        assertThat(category.isActive()).isTrue();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Category.create("  ", 1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeDisplayOrder() {
        assertThatThrownBy(() -> Category.create("X", -1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canBeRenamedAndReordered() {
        Category category = Category.create("Old", 1);
        category.rename("New");
        category.reorder(5);
        assertThat(category.name()).isEqualTo("New");
        assertThat(category.displayOrder()).isEqualTo(5);
    }

    @Test
    void deactivateAndReactivate() {
        Category category = Category.create("X", 1);
        category.deactivate();
        assertThat(category.isActive()).isFalse();
        category.activate();
        assertThat(category.isActive()).isTrue();
    }
}
```

- [ ] **Step 3.2:** Implement `Category`

File: `src/main/java/com/cardapio/catalog/domain/model/Category.java`

```java
package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.AggregateRoot;

import java.util.Objects;

public final class Category extends AggregateRoot<CategoryId> {

    private String name;
    private int displayOrder;
    private boolean active;

    private Category(CategoryId id, String name, int displayOrder, boolean active) {
        super(id);
        setName(name);
        setDisplayOrder(displayOrder);
        this.active = active;
    }

    public static Category create(String name, int displayOrder) {
        return new Category(CategoryId.newId(), name, displayOrder, true);
    }

    public static Category rehydrate(CategoryId id, String name, int displayOrder, boolean active) {
        return new Category(id, name, displayOrder, active);
    }

    public void rename(String name) { setName(name); }
    public void reorder(int displayOrder) { setDisplayOrder(displayOrder); }
    public void activate() { this.active = true; }
    public void deactivate() { this.active = false; }

    private void setName(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("name must not be blank");
        this.name = trimmed;
    }

    private void setDisplayOrder(int order) {
        if (order < 0) throw new IllegalArgumentException("displayOrder must be non-negative");
        this.displayOrder = order;
    }

    public String name() { return name; }
    public int displayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
}
```

- [ ] **Step 3.3:** `CategoryNotFoundException`

File: `src/main/java/com/cardapio/catalog/domain/exception/CategoryNotFoundException.java`

```java
package com.cardapio.catalog.domain.exception;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.shared.domain.DomainException;

public class CategoryNotFoundException extends DomainException {
    public CategoryNotFoundException(CategoryId id) {
        super("CATEGORY_NOT_FOUND", "category not found: " + id.value());
    }
}
```

- [ ] **Step 3.4:** Run, pass, commit

```bash
./mvnw test -Dtest=CategoryTest
git add src/main/java/com/cardapio/catalog/domain src/test/java/com/cardapio/catalog/domain/model/CategoryTest.java
git commit -m "feat(catalog): add Category aggregate"
```

---

## Task 4: Product children entities (Variation, AddOnGroup, AddOnItem)

These are entities **within** the Product aggregate. They have IDs but cannot exist without a Product.

**Files (all under `src/main/java/com/cardapio/catalog/domain/model/`):**
- `Variation.java`
- `AddOnItem.java`
- `AddOnGroup.java`
- Test: `src/test/java/com/cardapio/catalog/domain/model/AddOnGroupTest.java`

- [ ] **Step 4.1:** `Variation`

```java
package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.Money;

import java.util.Objects;

public final class Variation {

    private final VariationId id;
    private String name;
    private Money priceModifier;  // can be 0; added to product basePrice

    private Variation(VariationId id, String name, Money priceModifier) {
        this.id = Objects.requireNonNull(id, "id");
        rename(name);
        repriceBy(priceModifier);
    }

    public static Variation create(String name, Money priceModifier) {
        return new Variation(VariationId.newId(), name, priceModifier);
    }

    public static Variation rehydrate(VariationId id, String name, Money priceModifier) {
        return new Variation(id, name, priceModifier);
    }

    public void rename(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("variation name must not be blank");
        this.name = trimmed;
    }

    public void repriceBy(Money priceModifier) {
        this.priceModifier = Objects.requireNonNull(priceModifier, "priceModifier");
    }

    public VariationId id() { return id; }
    public String name() { return name; }
    public Money priceModifier() { return priceModifier; }
}
```

- [ ] **Step 4.2:** `AddOnItem`

```java
package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.Money;

import java.util.Objects;

public final class AddOnItem {

    private final AddOnItemId id;
    private String name;
    private Money price;

    private AddOnItem(AddOnItemId id, String name, Money price) {
        this.id = Objects.requireNonNull(id, "id");
        rename(name);
        reprice(price);
    }

    public static AddOnItem create(String name, Money price) {
        return new AddOnItem(AddOnItemId.newId(), name, price);
    }

    public static AddOnItem rehydrate(AddOnItemId id, String name, Money price) {
        return new AddOnItem(id, name, price);
    }

    public void rename(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("addon item name must not be blank");
        this.name = trimmed;
    }

    public void reprice(Money price) { this.price = Objects.requireNonNull(price, "price"); }

    public AddOnItemId id() { return id; }
    public String name() { return name; }
    public Money price() { return price; }
}
```

- [ ] **Step 4.3:** `AddOnGroup`

```java
package com.cardapio.catalog.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class AddOnGroup {

    private final AddOnGroupId id;
    private String name;
    private int minSelection;
    private int maxSelection;
    private final List<AddOnItem> items;

    private AddOnGroup(AddOnGroupId id, String name, int minSelection, int maxSelection, List<AddOnItem> items) {
        this.id = Objects.requireNonNull(id, "id");
        rename(name);
        configureSelectionBounds(minSelection, maxSelection);
        this.items = new ArrayList<>(Objects.requireNonNull(items, "items"));
    }

    public static AddOnGroup create(String name, int minSelection, int maxSelection) {
        return new AddOnGroup(AddOnGroupId.newId(), name, minSelection, maxSelection, new ArrayList<>());
    }

    public static AddOnGroup rehydrate(AddOnGroupId id, String name, int minSelection, int maxSelection, List<AddOnItem> items) {
        return new AddOnGroup(id, name, minSelection, maxSelection, items);
    }

    public void rename(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("addon group name must not be blank");
        this.name = trimmed;
    }

    public void configureSelectionBounds(int min, int max) {
        if (min < 0) throw new IllegalArgumentException("min selection must be non-negative");
        if (max < min) throw new IllegalArgumentException("max selection must be >= min");
        this.minSelection = min;
        this.maxSelection = max;
    }

    public void addItem(AddOnItem item) { items.add(Objects.requireNonNull(item)); }

    public void removeItem(AddOnItemId itemId) {
        items.removeIf(i -> i.id().equals(itemId));
    }

    public AddOnGroupId id() { return id; }
    public String name() { return name; }
    public int minSelection() { return minSelection; }
    public int maxSelection() { return maxSelection; }
    public List<AddOnItem> items() { return Collections.unmodifiableList(items); }
}
```

- [ ] **Step 4.4:** Test for `AddOnGroup`

File: `src/test/java/com/cardapio/catalog/domain/model/AddOnGroupTest.java`

```java
package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddOnGroupTest {

    @Test
    void createsEmptyGroup() {
        AddOnGroup g = AddOnGroup.create("Adicionais", 0, 5);
        assertThat(g.items()).isEmpty();
        assertThat(g.minSelection()).isEqualTo(0);
        assertThat(g.maxSelection()).isEqualTo(5);
    }

    @Test
    void addsAndRemovesItems() {
        AddOnGroup g = AddOnGroup.create("Adicionais", 0, 5);
        AddOnItem bacon = AddOnItem.create("Bacon", Money.brl("3.00"));
        AddOnItem cheese = AddOnItem.create("Queijo extra", Money.brl("2.50"));
        g.addItem(bacon);
        g.addItem(cheese);
        assertThat(g.items()).hasSize(2);

        g.removeItem(bacon.id());
        assertThat(g.items()).hasSize(1);
        assertThat(g.items().get(0).name()).isEqualTo("Queijo extra");
    }

    @Test
    void itemsListIsImmutableFromOutside() {
        AddOnGroup g = AddOnGroup.create("X", 0, 1);
        var items = g.items();
        assertThatThrownBy(() -> items.add(AddOnItem.create("Y", Money.brl("1.00"))))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsMaxLessThanMin() {
        assertThatThrownBy(() -> AddOnGroup.create("X", 3, 1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 4.5:** Run, pass, commit

```bash
./mvnw test -Dtest=AddOnGroupTest
git add src/main/java/com/cardapio/catalog/domain/model/Variation.java \
        src/main/java/com/cardapio/catalog/domain/model/AddOnItem.java \
        src/main/java/com/cardapio/catalog/domain/model/AddOnGroup.java \
        src/test/java/com/cardapio/catalog/domain/model/AddOnGroupTest.java
git commit -m "feat(catalog): add Variation, AddOnGroup, AddOnItem entities"
```

---

## Task 5: Product aggregate

**Files:**
- Create: `src/main/java/com/cardapio/catalog/domain/model/Product.java`
- Create: `src/main/java/com/cardapio/catalog/domain/exception/ProductNotFoundException.java`
- Test: `src/test/java/com/cardapio/catalog/domain/model/ProductTest.java`

- [ ] **Step 5.1:** Test

File: `src/test/java/com/cardapio/catalog/domain/model/ProductTest.java`

```java
package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    void createsAvailableProductWithBasePrice() {
        CategoryId category = CategoryId.newId();
        Product p = Product.create(
            "Pizza Margherita", "Molho, mussarela, manjericão",
            Money.brl("39.90"), category, null, false);

        assertThat(p.id()).isNotNull();
        assertThat(p.name()).isEqualTo("Pizza Margherita");
        assertThat(p.basePrice()).isEqualTo(Money.brl("39.90"));
        assertThat(p.categoryId()).isEqualTo(category);
        assertThat(p.isAvailable()).isTrue();
        assertThat(p.allowsHalfHalf()).isFalse();
        assertThat(p.stock().isTracked()).isFalse();
        assertThat(p.variations()).isEmpty();
        assertThat(p.addOnGroups()).isEmpty();
    }

    @Test
    void allowsHalfHalfWhenFlagged() {
        Product p = Product.create("Pizza", "desc", Money.brl("39.90"),
            CategoryId.newId(), null, true);
        assertThat(p.allowsHalfHalf()).isTrue();
    }

    @Test
    void supportsVariationsAndAddOns() {
        Product p = Product.create("Pizza", "desc", Money.brl("39.90"),
            CategoryId.newId(), null, true);

        Variation small = Variation.create("Pequena", Money.brl("0.00"));
        Variation large = Variation.create("Grande", Money.brl("10.00"));
        p.addVariation(small);
        p.addVariation(large);

        AddOnGroup extras = AddOnGroup.create("Adicionais", 0, 3);
        extras.addItem(AddOnItem.create("Bacon", Money.brl("3.00")));
        p.addAddOnGroup(extras);

        assertThat(p.variations()).hasSize(2);
        assertThat(p.addOnGroups()).hasSize(1);
    }

    @Test
    void availabilityToggle() {
        Product p = Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false);
        p.markUnavailable();
        assertThat(p.isAvailable()).isFalse();
        p.markAvailable();
        assertThat(p.isAvailable()).isTrue();
    }

    @Test
    void setStockReplacesValue() {
        Product p = Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false);
        p.changeStock(Stock.of(50));
        assertThat(p.stock().quantity()).isEqualTo(50);
        p.changeStock(Stock.untracked());
        assertThat(p.stock().isTracked()).isFalse();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Product.create("  ", "desc", Money.brl("1.00"), CategoryId.newId(), null, false))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeVariation() {
        Product p = Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false);
        Variation v = Variation.create("M", Money.brl("0.00"));
        p.addVariation(v);
        p.removeVariation(v.id());
        assertThat(p.variations()).isEmpty();
    }
}
```

- [ ] **Step 5.2:** Implement `Product`

File: `src/main/java/com/cardapio/catalog/domain/model/Product.java`

```java
package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.AggregateRoot;
import com.cardapio.shared.domain.Money;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Product extends AggregateRoot<ProductId> {

    private String name;
    private String description;
    private Money basePrice;
    private CategoryId categoryId;
    private String imageUrl;          // nullable
    private boolean available;
    private boolean allowsHalfHalf;
    private Stock stock;
    private final List<Variation> variations;
    private final List<AddOnGroup> addOnGroups;

    private Product(ProductId id, String name, String description, Money basePrice,
                    CategoryId categoryId, String imageUrl, boolean available, boolean allowsHalfHalf,
                    Stock stock, List<Variation> variations, List<AddOnGroup> addOnGroups) {
        super(id);
        rename(name);
        changeDescription(description);
        repriceBase(basePrice);
        moveToCategory(categoryId);
        this.imageUrl = imageUrl;
        this.available = available;
        this.allowsHalfHalf = allowsHalfHalf;
        this.stock = Objects.requireNonNull(stock, "stock");
        this.variations = new ArrayList<>(Objects.requireNonNull(variations, "variations"));
        this.addOnGroups = new ArrayList<>(Objects.requireNonNull(addOnGroups, "addOnGroups"));
    }

    public static Product create(String name, String description, Money basePrice,
                                 CategoryId categoryId, String imageUrl, boolean allowsHalfHalf) {
        return new Product(ProductId.newId(), name, description, basePrice, categoryId, imageUrl,
            true, allowsHalfHalf, Stock.untracked(), new ArrayList<>(), new ArrayList<>());
    }

    public static Product rehydrate(ProductId id, String name, String description, Money basePrice,
                                    CategoryId categoryId, String imageUrl, boolean available, boolean allowsHalfHalf,
                                    Stock stock, List<Variation> variations, List<AddOnGroup> addOnGroups) {
        return new Product(id, name, description, basePrice, categoryId, imageUrl, available, allowsHalfHalf,
            stock, variations, addOnGroups);
    }

    public void rename(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("product name must not be blank");
        this.name = trimmed;
    }

    public void changeDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public void repriceBase(Money basePrice) {
        this.basePrice = Objects.requireNonNull(basePrice, "basePrice");
    }

    public void moveToCategory(CategoryId categoryId) {
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
    }

    public void changeImage(String imageUrl) { this.imageUrl = imageUrl; }
    public void allowHalfHalf() { this.allowsHalfHalf = true; }
    public void disallowHalfHalf() { this.allowsHalfHalf = false; }

    public void markAvailable() { this.available = true; }
    public void markUnavailable() { this.available = false; }

    public void changeStock(Stock stock) { this.stock = Objects.requireNonNull(stock, "stock"); }

    public void addVariation(Variation v) { variations.add(Objects.requireNonNull(v)); }
    public void removeVariation(VariationId id) { variations.removeIf(v -> v.id().equals(id)); }

    public void addAddOnGroup(AddOnGroup g) { addOnGroups.add(Objects.requireNonNull(g)); }
    public void removeAddOnGroup(AddOnGroupId id) { addOnGroups.removeIf(g -> g.id().equals(id)); }

    public String name() { return name; }
    public String description() { return description; }
    public Money basePrice() { return basePrice; }
    public CategoryId categoryId() { return categoryId; }
    public String imageUrl() { return imageUrl; }
    public boolean isAvailable() { return available; }
    public boolean allowsHalfHalf() { return allowsHalfHalf; }
    public Stock stock() { return stock; }
    public List<Variation> variations() { return Collections.unmodifiableList(variations); }
    public List<AddOnGroup> addOnGroups() { return Collections.unmodifiableList(addOnGroups); }
}
```

- [ ] **Step 5.3:** `ProductNotFoundException`

File: `src/main/java/com/cardapio/catalog/domain/exception/ProductNotFoundException.java`

```java
package com.cardapio.catalog.domain.exception;

import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.shared.domain.DomainException;

public class ProductNotFoundException extends DomainException {
    public ProductNotFoundException(ProductId id) {
        super("PRODUCT_NOT_FOUND", "product not found: " + id.value());
    }
}
```

- [ ] **Step 5.4:** Run, pass, commit

```bash
./mvnw test -Dtest=ProductTest
git add src/main/java/com/cardapio/catalog/domain src/test/java/com/cardapio/catalog/domain/model/ProductTest.java
git commit -m "feat(catalog): add Product aggregate with variations, addons, half-half, stock"
```

---

## Task 6: OperatingHours aggregate

**Files:**
- Create: `src/main/java/com/cardapio/catalog/domain/model/DayHours.java`
- Create: `src/main/java/com/cardapio/catalog/domain/model/OperatingHours.java`
- Test: `src/test/java/com/cardapio/catalog/domain/model/OperatingHoursTest.java`

OperatingHours is a singleton-style aggregate (only one per restaurant). It has a fixed ID we'll define as `OperatingHours.SINGLETON_ID`.

- [ ] **Step 6.1:** Test

```java
package com.cardapio.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperatingHoursTest {

    @Test
    void emptyOperatingHoursIsClosedAlways() {
        OperatingHours hours = OperatingHours.empty();
        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 4, 12, 0))).isFalse();
    }

    @Test
    void setHoursForSingleDay() {
        OperatingHours hours = OperatingHours.empty();
        hours.setHoursFor(DayOfWeek.MONDAY,
            List.of(TimeRange.of(LocalTime.of(11, 0), LocalTime.of(15, 0)),
                    TimeRange.of(LocalTime.of(18, 0), LocalTime.of(23, 0))));

        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 4, 12, 0))).isTrue();   // Monday lunch
        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 4, 16, 0))).isFalse();  // Monday gap
        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 4, 22, 0))).isTrue();   // Monday dinner
        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 5, 12, 0))).isFalse();  // Tuesday — not configured
    }

    @Test
    void replacingDayHoursOverrides() {
        OperatingHours hours = OperatingHours.empty();
        hours.setHoursFor(DayOfWeek.MONDAY, List.of(TimeRange.of(LocalTime.of(8, 0), LocalTime.of(10, 0))));
        hours.setHoursFor(DayOfWeek.MONDAY, List.of(TimeRange.of(LocalTime.of(11, 0), LocalTime.of(15, 0))));

        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 4, 9, 0))).isFalse();
        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 4, 12, 0))).isTrue();
    }

    @Test
    void exposesHoursForAllDays() {
        OperatingHours hours = OperatingHours.empty();
        hours.setHoursFor(DayOfWeek.WEDNESDAY,
            List.of(TimeRange.of(LocalTime.of(9, 0), LocalTime.of(17, 0))));

        DayHours wed = hours.hoursFor(DayOfWeek.WEDNESDAY);
        assertThat(wed.intervals()).hasSize(1);
    }
}
```

- [ ] **Step 6.2:** Implement `DayHours`

File: `src/main/java/com/cardapio/catalog/domain/model/DayHours.java`

```java
package com.cardapio.catalog.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class DayHours {

    private final DayOfWeek dayOfWeek;
    private final List<TimeRange> intervals;

    public DayHours(DayOfWeek dayOfWeek, List<TimeRange> intervals) {
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek");
        this.intervals = List.copyOf(Objects.requireNonNull(intervals, "intervals"));
    }

    public boolean contains(LocalTime time) {
        return intervals.stream().anyMatch(r -> r.contains(time));
    }

    public DayOfWeek dayOfWeek() { return dayOfWeek; }
    public List<TimeRange> intervals() { return Collections.unmodifiableList(intervals); }
}
```

- [ ] **Step 6.3:** Implement `OperatingHours`

File: `src/main/java/com/cardapio/catalog/domain/model/OperatingHours.java`

```java
package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.AggregateRoot;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OperatingHours extends AggregateRoot<UUID> {

    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final Map<DayOfWeek, DayHours> hoursByDay;

    private OperatingHours(UUID id, Map<DayOfWeek, DayHours> hoursByDay) {
        super(id);
        this.hoursByDay = new EnumMap<>(hoursByDay);
    }

    public static OperatingHours empty() {
        return new OperatingHours(SINGLETON_ID, new EnumMap<>(DayOfWeek.class));
    }

    public static OperatingHours rehydrate(Map<DayOfWeek, DayHours> hoursByDay) {
        return new OperatingHours(SINGLETON_ID, hoursByDay);
    }

    public void setHoursFor(DayOfWeek day, List<TimeRange> intervals) {
        if (intervals == null || intervals.isEmpty()) {
            hoursByDay.remove(day);
        } else {
            hoursByDay.put(day, new DayHours(day, intervals));
        }
    }

    public DayHours hoursFor(DayOfWeek day) {
        return hoursByDay.getOrDefault(day, new DayHours(day, List.of()));
    }

    public boolean isOpenAt(LocalDateTime when) {
        DayHours dh = hoursByDay.get(when.getDayOfWeek());
        return dh != null && dh.contains(when.toLocalTime());
    }

    public Map<DayOfWeek, DayHours> snapshot() { return Map.copyOf(hoursByDay); }
}
```

- [ ] **Step 6.4:** Run, pass, commit

```bash
./mvnw test -Dtest=OperatingHoursTest
git add src/main/java/com/cardapio/catalog/domain/model/DayHours.java \
        src/main/java/com/cardapio/catalog/domain/model/OperatingHours.java \
        src/test/java/com/cardapio/catalog/domain/model/OperatingHoursTest.java
git commit -m "feat(catalog): add OperatingHours aggregate with weekday intervals"
```

---

## Task 7: Domain ports

**Files (all under `src/main/java/com/cardapio/catalog/domain/port/`):**
- `CategoryRepository.java`
- `ProductRepository.java`
- `OperatingHoursRepository.java`

- [ ] **Step 7.1:** `CategoryRepository`

```java
package com.cardapio.catalog.domain.port;

import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    void save(Category category);
    Optional<Category> findById(CategoryId id);
    List<Category> findAll();
    List<Category> findAllActive();
    void deleteById(CategoryId id);
    boolean existsById(CategoryId id);
}
```

- [ ] **Step 7.2:** `ProductRepository`

```java
package com.cardapio.catalog.domain.port;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.model.ProductId;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    void save(Product product);
    Optional<Product> findById(ProductId id);
    List<Product> findAll();
    List<Product> findAvailableByCategory(CategoryId categoryId);
    void deleteById(ProductId id);
    long countByCategory(CategoryId categoryId);
}
```

- [ ] **Step 7.3:** `OperatingHoursRepository`

```java
package com.cardapio.catalog.domain.port;

import com.cardapio.catalog.domain.model.OperatingHours;

import java.util.Optional;

public interface OperatingHoursRepository {
    void save(OperatingHours hours);
    Optional<OperatingHours> load();
}
```

- [ ] **Step 7.4:** Compile + commit

```bash
./mvnw -DskipTests compile
git add src/main/java/com/cardapio/catalog/domain/port
git commit -m "feat(catalog): add domain ports (3 repositories)"
```

---

## Task 8: Flyway migration V4 — catalog tables

**Files:**
- Create: `src/main/resources/db/migration/V4__catalog_tables.sql`

- [ ] **Step 8.1:** Migration

```sql
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_categories_display_order ON categories (display_order);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES categories(id),
    name VARCHAR(180) NOT NULL,
    description VARCHAR(2000) NOT NULL DEFAULT '',
    base_price NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    image_url VARCHAR(500),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    allows_half_half BOOLEAN NOT NULL DEFAULT FALSE,
    stock_quantity INTEGER,                       -- NULL = untracked
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_available ON products (available) WHERE available = TRUE;

CREATE TABLE product_variations (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    price_modifier NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    position INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_variations_product ON product_variations (product_id);

CREATE TABLE addon_groups (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    min_selection INTEGER NOT NULL DEFAULT 0,
    max_selection INTEGER NOT NULL DEFAULT 1,
    position INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_addon_groups_product ON addon_groups (product_id);

CREATE TABLE addon_items (
    id UUID PRIMARY KEY,
    addon_group_id UUID NOT NULL REFERENCES addon_groups(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    position INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_addon_items_group ON addon_items (addon_group_id);

CREATE TABLE operating_hours (
    id UUID PRIMARY KEY,
    day_of_week SMALLINT NOT NULL,                -- 1=MONDAY .. 7=SUNDAY (java.time.DayOfWeek)
    open_time TIME NOT NULL,
    close_time TIME NOT NULL
);
CREATE INDEX idx_operating_hours_day ON operating_hours (day_of_week);
```

NOTE: `operating_hours` stores one row per `(day, interval)` — the aggregate ID is implicit (the OperatingHours singleton). The PK `id` is the row ID, not the aggregate ID.

- [ ] **Step 8.2:** Run integration test to apply migration

Run: `./mvnw test -Dtest=SmokeIntegrationTest`
Expected: PASS — Flyway applies V4 alongside V1/V2/V3.

- [ ] **Step 8.3:** Commit

```bash
git add src/main/resources/db/migration/V4__catalog_tables.sql
git commit -m "feat(catalog): add Flyway V4 migration for catalog tables"
```

---

## Task 9: JPA persistence layer (Category)

**Files:**
- `src/main/java/com/cardapio/catalog/infrastructure/persistence/jpa/CategoryJpaEntity.java`
- `src/main/java/com/cardapio/catalog/infrastructure/persistence/repository/SpringCategoryJpaRepository.java`
- `src/main/java/com/cardapio/catalog/infrastructure/persistence/mapper/CategoryMapper.java`
- `src/main/java/com/cardapio/catalog/infrastructure/persistence/adapter/CategoryRepositoryAdapter.java`

- [ ] **Step 9.1:** `CategoryJpaEntity`

```java
package com.cardapio.catalog.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class CategoryJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected CategoryJpaEntity() {}
    public CategoryJpaEntity(UUID id, String name, int displayOrder, boolean active, Instant createdAt) {
        this.id = id; this.name = name; this.displayOrder = displayOrder; this.active = active; this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public void setActive(boolean active) { this.active = active; }
}
```

- [ ] **Step 9.2:** `SpringCategoryJpaRepository`

```java
package com.cardapio.catalog.infrastructure.persistence.repository;

import com.cardapio.catalog.infrastructure.persistence.jpa.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringCategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {
    List<CategoryJpaEntity> findAllByOrderByDisplayOrderAsc();
    List<CategoryJpaEntity> findAllByActiveTrueOrderByDisplayOrderAsc();
}
```

- [ ] **Step 9.3:** `CategoryMapper`

```java
package com.cardapio.catalog.infrastructure.persistence.mapper;

import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.infrastructure.persistence.jpa.CategoryJpaEntity;

import java.time.Instant;

public final class CategoryMapper {
    private CategoryMapper() {}

    public static CategoryJpaEntity toJpa(Category c, Instant now) {
        return new CategoryJpaEntity(c.id().value(), c.name(), c.displayOrder(), c.isActive(), now);
    }

    public static void updateJpa(CategoryJpaEntity entity, Category c) {
        entity.setName(c.name());
        entity.setDisplayOrder(c.displayOrder());
        entity.setActive(c.isActive());
    }

    public static Category toDomain(CategoryJpaEntity e) {
        return Category.rehydrate(CategoryId.of(e.getId()), e.getName(), e.getDisplayOrder(), e.isActive());
    }
}
```

- [ ] **Step 9.4:** `CategoryRepositoryAdapter`

```java
package com.cardapio.catalog.infrastructure.persistence.adapter;

import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.infrastructure.persistence.mapper.CategoryMapper;
import com.cardapio.catalog.infrastructure.persistence.repository.SpringCategoryJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Component
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final SpringCategoryJpaRepository jpa;
    private final Clock clock;

    public CategoryRepositoryAdapter(SpringCategoryJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public void save(Category category) {
        var existing = jpa.findById(category.id().value());
        if (existing.isPresent()) {
            CategoryMapper.updateJpa(existing.get(), category);
            jpa.save(existing.get());
        } else {
            jpa.save(CategoryMapper.toJpa(category, clock.instant()));
        }
    }

    @Override public Optional<Category> findById(CategoryId id) {
        return jpa.findById(id.value()).map(CategoryMapper::toDomain);
    }

    @Override public List<Category> findAll() {
        return jpa.findAllByOrderByDisplayOrderAsc().stream().map(CategoryMapper::toDomain).toList();
    }

    @Override public List<Category> findAllActive() {
        return jpa.findAllByActiveTrueOrderByDisplayOrderAsc().stream().map(CategoryMapper::toDomain).toList();
    }

    @Override public void deleteById(CategoryId id) { jpa.deleteById(id.value()); }
    @Override public boolean existsById(CategoryId id) { return jpa.existsById(id.value()); }
}
```

- [ ] **Step 9.5:** Run integration test (just to verify JPA validates)

Run: `./mvnw test -Dtest=SmokeIntegrationTest`
Expected: PASS — Hibernate validates `CategoryJpaEntity` against the V4 `categories` table.

- [ ] **Step 9.6:** Commit

```bash
git add src/main/java/com/cardapio/catalog/infrastructure/persistence
git commit -m "feat(catalog): add JPA persistence for Category"
```

---

## Task 10: JPA persistence layer (Product + children + OperatingHours)

This is the largest task. Product has 4 cascading children (variations, addon groups, addon items inside groups).

**Files (all under `src/main/java/com/cardapio/catalog/infrastructure/persistence/`):**
- `jpa/ProductJpaEntity.java`, `jpa/VariationJpaEntity.java`, `jpa/AddOnGroupJpaEntity.java`, `jpa/AddOnItemJpaEntity.java`
- `jpa/OperatingHoursJpaEntity.java`
- `repository/SpringProductJpaRepository.java`, `repository/SpringOperatingHoursJpaRepository.java`
- `mapper/ProductMapper.java`, `mapper/OperatingHoursMapper.java`
- `adapter/ProductRepositoryAdapter.java`, `adapter/OperatingHoursRepositoryAdapter.java`

- [ ] **Step 10.1:** `ProductJpaEntity` (with cascade to children)

```java
package com.cardapio.catalog.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "category_id", nullable = false) private UUID categoryId;
    @Column(nullable = false, length = 180) private String name;
    @Column(nullable = false, length = 2000) private String description;
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2) private BigDecimal basePrice;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "image_url", length = 500) private String imageUrl;
    @Column(nullable = false) private boolean available;
    @Column(name = "allows_half_half", nullable = false) private boolean allowsHalfHalf;
    @Column(name = "stock_quantity") private Integer stockQuantity;  // null = untracked
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<VariationJpaEntity> variations = new ArrayList<>();

    @OneToMany(mappedBy = "productId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<AddOnGroupJpaEntity> addOnGroups = new ArrayList<>();

    protected ProductJpaEntity() {}

    public ProductJpaEntity(UUID id, UUID categoryId, String name, String description,
                            BigDecimal basePrice, String currency, String imageUrl,
                            boolean available, boolean allowsHalfHalf, Integer stockQuantity,
                            Instant createdAt, Instant updatedAt) {
        this.id = id; this.categoryId = categoryId; this.name = name; this.description = description;
        this.basePrice = basePrice; this.currency = currency; this.imageUrl = imageUrl;
        this.available = available; this.allowsHalfHalf = allowsHalfHalf; this.stockQuantity = stockQuantity;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getBasePrice() { return basePrice; }
    public String getCurrency() { return currency; }
    public String getImageUrl() { return imageUrl; }
    public boolean isAvailable() { return available; }
    public boolean isAllowsHalfHalf() { return allowsHalfHalf; }
    public Integer getStockQuantity() { return stockQuantity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<VariationJpaEntity> getVariations() { return variations; }
    public List<AddOnGroupJpaEntity> getAddOnGroups() { return addOnGroups; }

    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setAllowsHalfHalf(boolean allowsHalfHalf) { this.allowsHalfHalf = allowsHalfHalf; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 10.2:** `VariationJpaEntity`

```java
package com.cardapio.catalog.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_variations")
public class VariationJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(nullable = false, length = 80) private String name;
    @Column(name = "price_modifier", nullable = false, precision = 12, scale = 2) private BigDecimal priceModifier;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false) private int position;

    protected VariationJpaEntity() {}

    public VariationJpaEntity(UUID id, UUID productId, String name, BigDecimal priceModifier, String currency, int position) {
        this.id = id; this.productId = productId; this.name = name;
        this.priceModifier = priceModifier; this.currency = currency; this.position = position;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getName() { return name; }
    public BigDecimal getPriceModifier() { return priceModifier; }
    public String getCurrency() { return currency; }
    public int getPosition() { return position; }
}
```

- [ ] **Step 10.3:** `AddOnGroupJpaEntity`

```java
package com.cardapio.catalog.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "addon_groups")
public class AddOnGroupJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "min_selection", nullable = false) private int minSelection;
    @Column(name = "max_selection", nullable = false) private int maxSelection;
    @Column(nullable = false) private int position;

    @OneToMany(mappedBy = "groupId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<AddOnItemJpaEntity> items = new ArrayList<>();

    protected AddOnGroupJpaEntity() {}

    public AddOnGroupJpaEntity(UUID id, UUID productId, String name, int minSelection, int maxSelection, int position) {
        this.id = id; this.productId = productId; this.name = name;
        this.minSelection = minSelection; this.maxSelection = maxSelection; this.position = position;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getName() { return name; }
    public int getMinSelection() { return minSelection; }
    public int getMaxSelection() { return maxSelection; }
    public int getPosition() { return position; }
    public List<AddOnItemJpaEntity> getItems() { return items; }
}
```

- [ ] **Step 10.4:** `AddOnItemJpaEntity`

```java
package com.cardapio.catalog.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "addon_items")
public class AddOnItemJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "addon_group_id", nullable = false) private UUID groupId;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false) private int position;

    protected AddOnItemJpaEntity() {}

    public AddOnItemJpaEntity(UUID id, UUID groupId, String name, BigDecimal price, String currency, int position) {
        this.id = id; this.groupId = groupId; this.name = name;
        this.price = price; this.currency = currency; this.position = position;
    }

    public UUID getId() { return id; }
    public UUID getGroupId() { return groupId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public int getPosition() { return position; }
}
```

- [ ] **Step 10.5:** `OperatingHoursJpaEntity` (one row per (day, interval))

```java
package com.cardapio.catalog.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "operating_hours")
public class OperatingHoursJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "day_of_week", nullable = false) private short dayOfWeek;  // 1..7
    @Column(name = "open_time", nullable = false) private LocalTime openTime;
    @Column(name = "close_time", nullable = false) private LocalTime closeTime;

    protected OperatingHoursJpaEntity() {}

    public OperatingHoursJpaEntity(UUID id, short dayOfWeek, LocalTime openTime, LocalTime closeTime) {
        this.id = id; this.dayOfWeek = dayOfWeek; this.openTime = openTime; this.closeTime = closeTime;
    }

    public UUID getId() { return id; }
    public short getDayOfWeek() { return dayOfWeek; }
    public LocalTime getOpenTime() { return openTime; }
    public LocalTime getCloseTime() { return closeTime; }
}
```

- [ ] **Step 10.6:** Spring Data repos

File: `src/main/java/com/cardapio/catalog/infrastructure/persistence/repository/SpringProductJpaRepository.java`

```java
package com.cardapio.catalog.infrastructure.persistence.repository;

import com.cardapio.catalog.infrastructure.persistence.jpa.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {
    List<ProductJpaEntity> findAllByCategoryIdAndAvailableTrueOrderByName(UUID categoryId);
    long countByCategoryId(UUID categoryId);
}
```

File: `src/main/java/com/cardapio/catalog/infrastructure/persistence/repository/SpringOperatingHoursJpaRepository.java`

```java
package com.cardapio.catalog.infrastructure.persistence.repository;

import com.cardapio.catalog.infrastructure.persistence.jpa.OperatingHoursJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringOperatingHoursJpaRepository extends JpaRepository<OperatingHoursJpaEntity, UUID> {
    List<OperatingHoursJpaEntity> findAllByOrderByDayOfWeekAscOpenTimeAsc();
    void deleteAllByDayOfWeek(short dayOfWeek);
}
```

- [ ] **Step 10.7:** `ProductMapper`

File: `src/main/java/com/cardapio/catalog/infrastructure/persistence/mapper/ProductMapper.java`

```java
package com.cardapio.catalog.infrastructure.persistence.mapper;

import com.cardapio.catalog.domain.model.*;
import com.cardapio.catalog.infrastructure.persistence.jpa.*;
import com.cardapio.shared.domain.Money;

import java.time.Instant;
import java.util.Currency;
import java.util.List;

public final class ProductMapper {
    private ProductMapper() {}

    public static ProductJpaEntity toJpa(Product p, Instant now) {
        ProductJpaEntity entity = new ProductJpaEntity(
            p.id().value(), p.categoryId().value(), p.name(), p.description(),
            p.basePrice().amount(), p.basePrice().currency().getCurrencyCode(),
            p.imageUrl(), p.isAvailable(), p.allowsHalfHalf(),
            p.stock().rawQuantity(), now, now);

        int pos = 0;
        for (Variation v : p.variations()) {
            entity.getVariations().add(new VariationJpaEntity(
                v.id().value(), p.id().value(), v.name(),
                v.priceModifier().amount(), v.priceModifier().currency().getCurrencyCode(), pos++));
        }
        pos = 0;
        for (AddOnGroup g : p.addOnGroups()) {
            AddOnGroupJpaEntity ge = new AddOnGroupJpaEntity(
                g.id().value(), p.id().value(), g.name(), g.minSelection(), g.maxSelection(), pos++);
            int ipos = 0;
            for (AddOnItem item : g.items()) {
                ge.getItems().add(new AddOnItemJpaEntity(
                    item.id().value(), g.id().value(), item.name(),
                    item.price().amount(), item.price().currency().getCurrencyCode(), ipos++));
            }
            entity.getAddOnGroups().add(ge);
        }
        return entity;
    }

    public static void updateJpa(ProductJpaEntity entity, Product p, Instant now) {
        entity.setCategoryId(p.categoryId().value());
        entity.setName(p.name());
        entity.setDescription(p.description());
        entity.setBasePrice(p.basePrice().amount());
        entity.setImageUrl(p.imageUrl());
        entity.setAvailable(p.isAvailable());
        entity.setAllowsHalfHalf(p.allowsHalfHalf());
        entity.setStockQuantity(p.stock().rawQuantity());
        entity.setUpdatedAt(now);

        // simple replacement strategy (orphanRemoval handles deletes)
        entity.getVariations().clear();
        int pos = 0;
        for (Variation v : p.variations()) {
            entity.getVariations().add(new VariationJpaEntity(
                v.id().value(), p.id().value(), v.name(),
                v.priceModifier().amount(), v.priceModifier().currency().getCurrencyCode(), pos++));
        }

        entity.getAddOnGroups().clear();
        pos = 0;
        for (AddOnGroup g : p.addOnGroups()) {
            AddOnGroupJpaEntity ge = new AddOnGroupJpaEntity(
                g.id().value(), p.id().value(), g.name(), g.minSelection(), g.maxSelection(), pos++);
            int ipos = 0;
            for (AddOnItem item : g.items()) {
                ge.getItems().add(new AddOnItemJpaEntity(
                    item.id().value(), g.id().value(), item.name(),
                    item.price().amount(), item.price().currency().getCurrencyCode(), ipos++));
            }
            entity.getAddOnGroups().add(ge);
        }
    }

    public static Product toDomain(ProductJpaEntity e) {
        Currency currency = Currency.getInstance(e.getCurrency());

        List<Variation> variations = e.getVariations().stream()
            .map(ve -> Variation.rehydrate(VariationId.of(ve.getId()), ve.getName(),
                Money.of(ve.getPriceModifier(), Currency.getInstance(ve.getCurrency()))))
            .toList();

        List<AddOnGroup> groups = e.getAddOnGroups().stream()
            .map(ge -> {
                List<AddOnItem> items = ge.getItems().stream()
                    .map(ie -> AddOnItem.rehydrate(AddOnItemId.of(ie.getId()), ie.getName(),
                        Money.of(ie.getPrice(), Currency.getInstance(ie.getCurrency()))))
                    .toList();
                return AddOnGroup.rehydrate(AddOnGroupId.of(ge.getId()), ge.getName(),
                    ge.getMinSelection(), ge.getMaxSelection(), items);
            })
            .toList();

        Stock stock = e.getStockQuantity() == null ? Stock.untracked() : Stock.of(e.getStockQuantity());

        return Product.rehydrate(
            ProductId.of(e.getId()), e.getName(), e.getDescription(),
            Money.of(e.getBasePrice(), currency),
            CategoryId.of(e.getCategoryId()), e.getImageUrl(),
            e.isAvailable(), e.isAllowsHalfHalf(), stock, variations, groups);
    }
}
```

- [ ] **Step 10.8:** `OperatingHoursMapper`

```java
package com.cardapio.catalog.infrastructure.persistence.mapper;

import com.cardapio.catalog.domain.model.DayHours;
import com.cardapio.catalog.domain.model.OperatingHours;
import com.cardapio.catalog.domain.model.TimeRange;
import com.cardapio.catalog.infrastructure.persistence.jpa.OperatingHoursJpaEntity;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OperatingHoursMapper {
    private OperatingHoursMapper() {}

    public static List<OperatingHoursJpaEntity> toJpaRows(OperatingHours hours) {
        List<OperatingHoursJpaEntity> rows = new ArrayList<>();
        for (Map.Entry<DayOfWeek, DayHours> entry : hours.snapshot().entrySet()) {
            short day = (short) entry.getKey().getValue();
            for (TimeRange range : entry.getValue().intervals()) {
                rows.add(new OperatingHoursJpaEntity(UUID.randomUUID(), day, range.openTime(), range.closeTime()));
            }
        }
        return rows;
    }

    public static OperatingHours toDomain(List<OperatingHoursJpaEntity> rows) {
        Map<DayOfWeek, List<TimeRange>> byDay = new EnumMap<>(DayOfWeek.class);
        for (OperatingHoursJpaEntity row : rows) {
            DayOfWeek day = DayOfWeek.of(row.getDayOfWeek());
            byDay.computeIfAbsent(day, k -> new ArrayList<>())
                 .add(TimeRange.of(row.getOpenTime(), row.getCloseTime()));
        }
        Map<DayOfWeek, DayHours> result = new EnumMap<>(DayOfWeek.class);
        byDay.forEach((d, ranges) -> result.put(d, new DayHours(d, ranges)));
        return OperatingHours.rehydrate(result);
    }
}
```

- [ ] **Step 10.9:** `ProductRepositoryAdapter`

```java
package com.cardapio.catalog.infrastructure.persistence.adapter;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.catalog.infrastructure.persistence.mapper.ProductMapper;
import com.cardapio.catalog.infrastructure.persistence.repository.SpringProductJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Component
public class ProductRepositoryAdapter implements ProductRepository {

    private final SpringProductJpaRepository jpa;
    private final Clock clock;

    public ProductRepositoryAdapter(SpringProductJpaRepository jpa, Clock clock) {
        this.jpa = jpa; this.clock = clock;
    }

    @Override
    public void save(Product product) {
        var existing = jpa.findById(product.id().value());
        if (existing.isPresent()) {
            ProductMapper.updateJpa(existing.get(), product, clock.instant());
            jpa.save(existing.get());
        } else {
            jpa.save(ProductMapper.toJpa(product, clock.instant()));
        }
    }

    @Override public Optional<Product> findById(ProductId id) {
        return jpa.findById(id.value()).map(ProductMapper::toDomain);
    }

    @Override public List<Product> findAll() {
        return jpa.findAll().stream().map(ProductMapper::toDomain).toList();
    }

    @Override public List<Product> findAvailableByCategory(CategoryId categoryId) {
        return jpa.findAllByCategoryIdAndAvailableTrueOrderByName(categoryId.value())
            .stream().map(ProductMapper::toDomain).toList();
    }

    @Override public void deleteById(ProductId id) { jpa.deleteById(id.value()); }
    @Override public long countByCategory(CategoryId categoryId) { return jpa.countByCategoryId(categoryId.value()); }
}
```

- [ ] **Step 10.10:** `OperatingHoursRepositoryAdapter`

```java
package com.cardapio.catalog.infrastructure.persistence.adapter;

import com.cardapio.catalog.domain.model.OperatingHours;
import com.cardapio.catalog.domain.port.OperatingHoursRepository;
import com.cardapio.catalog.infrastructure.persistence.mapper.OperatingHoursMapper;
import com.cardapio.catalog.infrastructure.persistence.repository.SpringOperatingHoursJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class OperatingHoursRepositoryAdapter implements OperatingHoursRepository {

    private final SpringOperatingHoursJpaRepository jpa;

    public OperatingHoursRepositoryAdapter(SpringOperatingHoursJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void save(OperatingHours hours) {
        // simple: drop all rows and rewrite
        jpa.deleteAllInBatch();
        jpa.saveAll(OperatingHoursMapper.toJpaRows(hours));
    }

    @Override
    public Optional<OperatingHours> load() {
        var rows = jpa.findAllByOrderByDayOfWeekAscOpenTimeAsc();
        return rows.isEmpty() ? Optional.empty() : Optional.of(OperatingHoursMapper.toDomain(rows));
    }
}
```

- [ ] **Step 10.11:** Run integration test

Run: `./mvnw test -Dtest=SmokeIntegrationTest`
Expected: PASS — all 5 new JPA entities validate against V4 schema.

- [ ] **Step 10.12:** Commit

```bash
git add src/main/java/com/cardapio/catalog/infrastructure
git commit -m "feat(catalog): add JPA persistence for Product, Variation, AddOn*, OperatingHours"
```

---

## Task 11: Category use cases (Create, Update, Delete)

**Files:**
- `src/main/java/com/cardapio/catalog/application/command/CreateCategoryCommand.java`
- `src/main/java/com/cardapio/catalog/application/command/UpdateCategoryCommand.java`
- `src/main/java/com/cardapio/catalog/application/usecase/CreateCategoryUseCase.java`
- `src/main/java/com/cardapio/catalog/application/usecase/UpdateCategoryUseCase.java`
- `src/main/java/com/cardapio/catalog/application/usecase/DeleteCategoryUseCase.java`
- Test: `src/test/java/com/cardapio/catalog/application/usecase/CategoryUseCasesTest.java`

- [ ] **Step 11.1:** Commands

```java
// CreateCategoryCommand.java
package com.cardapio.catalog.application.command;
public record CreateCategoryCommand(String name, int displayOrder) {}
```

```java
// UpdateCategoryCommand.java
package com.cardapio.catalog.application.command;

import com.cardapio.catalog.domain.model.CategoryId;
public record UpdateCategoryCommand(CategoryId id, String name, int displayOrder, boolean active) {}
```

- [ ] **Step 11.2:** Use cases

```java
// CreateCategoryUseCase.java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.CreateCategoryCommand;
import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCategoryUseCase {
    private final CategoryRepository repo;
    public CreateCategoryUseCase(CategoryRepository repo) { this.repo = repo; }

    @Transactional
    public Result<CategoryId> execute(CreateCategoryCommand cmd) {
        Notification n = Notification.empty();
        if (cmd.name() == null || cmd.name().isBlank()) n.addError("name", "BLANK_NAME", "nome obrigatório");
        if (cmd.displayOrder() < 0) n.addError("displayOrder", "INVALID_DISPLAY_ORDER", "ordem inválida");
        if (n.hasErrors()) return Result.failure(n);

        Category category = Category.create(cmd.name(), cmd.displayOrder());
        repo.save(category);
        return Result.success(category.id());
    }
}
```

```java
// UpdateCategoryUseCase.java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.UpdateCategoryCommand;
import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UpdateCategoryUseCase {
    private final CategoryRepository repo;
    public UpdateCategoryUseCase(CategoryRepository repo) { this.repo = repo; }

    @Transactional
    public Result<CategoryId> execute(UpdateCategoryCommand cmd) {
        Notification n = Notification.empty();
        Optional<Category> maybe = repo.findById(cmd.id());
        if (maybe.isEmpty()) {
            n.addError("CATEGORY_NOT_FOUND", "categoria não encontrada");
            return Result.failure(n);
        }
        if (cmd.name() == null || cmd.name().isBlank()) n.addError("name", "BLANK_NAME", "nome obrigatório");
        if (cmd.displayOrder() < 0) n.addError("displayOrder", "INVALID_DISPLAY_ORDER", "ordem inválida");
        if (n.hasErrors()) return Result.failure(n);

        Category c = maybe.get();
        c.rename(cmd.name());
        c.reorder(cmd.displayOrder());
        if (cmd.active()) c.activate(); else c.deactivate();
        repo.save(c);
        return Result.success(c.id());
    }
}
```

```java
// DeleteCategoryUseCase.java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteCategoryUseCase {
    private final CategoryRepository categories;
    private final ProductRepository products;

    public DeleteCategoryUseCase(CategoryRepository categories, ProductRepository products) {
        this.categories = categories; this.products = products;
    }

    @Transactional
    public Result<Void> execute(CategoryId id) {
        Notification n = Notification.empty();
        if (!categories.existsById(id)) {
            n.addError("CATEGORY_NOT_FOUND", "categoria não encontrada");
            return Result.failure(n);
        }
        long pCount = products.countByCategory(id);
        if (pCount > 0) {
            n.addError("CATEGORY_HAS_PRODUCTS", "categoria tem produtos vinculados");
            return Result.failure(n);
        }
        categories.deleteById(id);
        return Result.success(null);
    }
}
```

- [ ] **Step 11.3:** Test

```java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.CreateCategoryCommand;
import com.cardapio.catalog.application.command.UpdateCategoryCommand;
import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoryUseCasesTest {

    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);

    @Test
    void createValid() {
        Result<CategoryId> r = new CreateCategoryUseCase(categories).execute(new CreateCategoryCommand("Pizzas", 1));
        assertThat(r.isSuccess()).isTrue();
        verify(categories).save(any());
    }

    @Test
    void createRejectsBlankName() {
        Result<CategoryId> r = new CreateCategoryUseCase(categories).execute(new CreateCategoryCommand("  ", 1));
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    void updateExistingCategory() {
        CategoryId id = CategoryId.newId();
        Category existing = Category.rehydrate(id, "Old", 1, true);
        when(categories.findById(id)).thenReturn(Optional.of(existing));

        Result<CategoryId> r = new UpdateCategoryUseCase(categories).execute(
            new UpdateCategoryCommand(id, "New", 2, false));

        assertThat(r.isSuccess()).isTrue();
        verify(categories).save(existing);
        assertThat(existing.name()).isEqualTo("New");
        assertThat(existing.isActive()).isFalse();
    }

    @Test
    void updateMissingCategoryFails() {
        when(categories.findById(any())).thenReturn(Optional.empty());
        Result<CategoryId> r = new UpdateCategoryUseCase(categories).execute(
            new UpdateCategoryCommand(CategoryId.newId(), "X", 1, true));
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    void deleteEmptyCategory() {
        CategoryId id = CategoryId.newId();
        when(categories.existsById(id)).thenReturn(true);
        when(products.countByCategory(id)).thenReturn(0L);

        Result<Void> r = new DeleteCategoryUseCase(categories, products).execute(id);
        assertThat(r.isSuccess()).isTrue();
        verify(categories).deleteById(id);
    }

    @Test
    void deleteRejectsCategoryWithProducts() {
        CategoryId id = CategoryId.newId();
        when(categories.existsById(id)).thenReturn(true);
        when(products.countByCategory(id)).thenReturn(3L);

        Result<Void> r = new DeleteCategoryUseCase(categories, products).execute(id);
        assertThat(r.isSuccess()).isFalse();
        verify(categories, never()).deleteById(any());
    }
}
```

- [ ] **Step 11.4:** Run, pass, commit

```bash
./mvnw test -Dtest=CategoryUseCasesTest
git add src/main/java/com/cardapio/catalog/application src/test/java/com/cardapio/catalog/application
git commit -m "feat(catalog): add Category CRUD use cases"
```

---

## Task 12: Product use cases (Create, Update, SetAvailability, SetStock, Delete)

**Files:**
- 5 commands + 5 use cases under `application/`
- Test: `src/test/java/com/cardapio/catalog/application/usecase/ProductUseCasesTest.java`

- [ ] **Step 12.1:** Commands

```java
// CreateProductCommand.java
package com.cardapio.catalog.application.command;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.shared.domain.Money;

import java.util.List;

public record CreateProductCommand(
    String name,
    String description,
    Money basePrice,
    CategoryId categoryId,
    String imageUrl,
    boolean allowsHalfHalf,
    List<VariationDraft> variations,
    List<AddOnGroupDraft> addOnGroups
) {
    public record VariationDraft(String name, Money priceModifier) {}
    public record AddOnGroupDraft(String name, int minSelection, int maxSelection, List<AddOnItemDraft> items) {}
    public record AddOnItemDraft(String name, Money price) {}
}
```

```java
// UpdateProductCommand.java
package com.cardapio.catalog.application.command;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.shared.domain.Money;

import java.util.List;

public record UpdateProductCommand(
    ProductId id,
    String name,
    String description,
    Money basePrice,
    CategoryId categoryId,
    String imageUrl,
    boolean allowsHalfHalf,
    List<CreateProductCommand.VariationDraft> variations,
    List<CreateProductCommand.AddOnGroupDraft> addOnGroups
) {}
```

```java
// SetProductAvailabilityCommand.java
package com.cardapio.catalog.application.command;

import com.cardapio.catalog.domain.model.ProductId;
public record SetProductAvailabilityCommand(ProductId id, boolean available) {}
```

```java
// SetProductStockCommand.java
package com.cardapio.catalog.application.command;

import com.cardapio.catalog.domain.model.ProductId;
public record SetProductStockCommand(ProductId id, Integer quantity) {  // null = untracked
}
```

- [ ] **Step 12.2:** `CreateProductUseCase`

```java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.CreateProductCommand;
import com.cardapio.catalog.domain.model.*;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateProductUseCase {
    private final ProductRepository products;
    private final CategoryRepository categories;

    public CreateProductUseCase(ProductRepository products, CategoryRepository categories) {
        this.products = products; this.categories = categories;
    }

    @Transactional
    public Result<ProductId> execute(CreateProductCommand cmd) {
        Notification n = Notification.empty();
        if (cmd.name() == null || cmd.name().isBlank()) n.addError("name", "BLANK_NAME", "nome obrigatório");
        if (cmd.basePrice() == null) n.addError("basePrice", "INVALID_PRICE", "preço inválido");
        if (cmd.categoryId() == null || !categories.existsById(cmd.categoryId())) {
            n.addError("categoryId", "CATEGORY_NOT_FOUND", "categoria não existe");
        }
        if (n.hasErrors()) return Result.failure(n);

        Product p = Product.create(cmd.name(), cmd.description(), cmd.basePrice(),
            cmd.categoryId(), cmd.imageUrl(), cmd.allowsHalfHalf());

        if (cmd.variations() != null) {
            for (CreateProductCommand.VariationDraft v : cmd.variations()) {
                p.addVariation(Variation.create(v.name(), v.priceModifier()));
            }
        }
        if (cmd.addOnGroups() != null) {
            for (CreateProductCommand.AddOnGroupDraft g : cmd.addOnGroups()) {
                AddOnGroup group = AddOnGroup.create(g.name(), g.minSelection(), g.maxSelection());
                if (g.items() != null) {
                    for (CreateProductCommand.AddOnItemDraft item : g.items()) {
                        group.addItem(AddOnItem.create(item.name(), item.price()));
                    }
                }
                p.addAddOnGroup(group);
            }
        }
        products.save(p);
        return Result.success(p.id());
    }
}
```

- [ ] **Step 12.3:** `UpdateProductUseCase` (replaces variations/addons wholesale on update — simpler than partial update)

```java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.CreateProductCommand;
import com.cardapio.catalog.application.command.UpdateProductCommand;
import com.cardapio.catalog.domain.model.*;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UpdateProductUseCase {
    private final ProductRepository products;
    private final CategoryRepository categories;

    public UpdateProductUseCase(ProductRepository products, CategoryRepository categories) {
        this.products = products; this.categories = categories;
    }

    @Transactional
    public Result<ProductId> execute(UpdateProductCommand cmd) {
        Notification n = Notification.empty();
        Optional<Product> maybe = products.findById(cmd.id());
        if (maybe.isEmpty()) { n.addError("PRODUCT_NOT_FOUND", "produto não encontrado"); return Result.failure(n); }
        if (cmd.name() == null || cmd.name().isBlank()) n.addError("name", "BLANK_NAME", "nome obrigatório");
        if (cmd.basePrice() == null) n.addError("basePrice", "INVALID_PRICE", "preço inválido");
        if (cmd.categoryId() == null || !categories.existsById(cmd.categoryId())) {
            n.addError("categoryId", "CATEGORY_NOT_FOUND", "categoria não existe");
        }
        if (n.hasErrors()) return Result.failure(n);

        Product p = maybe.get();
        p.rename(cmd.name());
        p.changeDescription(cmd.description());
        p.repriceBase(cmd.basePrice());
        p.moveToCategory(cmd.categoryId());
        p.changeImage(cmd.imageUrl());
        if (cmd.allowsHalfHalf()) p.allowHalfHalf(); else p.disallowHalfHalf();

        // wholesale replacement of variations + addons
        for (Variation v : p.variations().stream().toList()) p.removeVariation(v.id());
        for (AddOnGroup g : p.addOnGroups().stream().toList()) p.removeAddOnGroup(g.id());

        if (cmd.variations() != null) {
            for (CreateProductCommand.VariationDraft v : cmd.variations()) {
                p.addVariation(Variation.create(v.name(), v.priceModifier()));
            }
        }
        if (cmd.addOnGroups() != null) {
            for (CreateProductCommand.AddOnGroupDraft g : cmd.addOnGroups()) {
                AddOnGroup group = AddOnGroup.create(g.name(), g.minSelection(), g.maxSelection());
                if (g.items() != null) {
                    for (CreateProductCommand.AddOnItemDraft item : g.items()) {
                        group.addItem(AddOnItem.create(item.name(), item.price()));
                    }
                }
                p.addAddOnGroup(group);
            }
        }
        products.save(p);
        return Result.success(p.id());
    }
}
```

- [ ] **Step 12.4:** `SetProductAvailabilityUseCase`

```java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.SetProductAvailabilityCommand;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SetProductAvailabilityUseCase {
    private final ProductRepository products;
    public SetProductAvailabilityUseCase(ProductRepository products) { this.products = products; }

    @Transactional
    public Result<Void> execute(SetProductAvailabilityCommand cmd) {
        Optional<Product> maybe = products.findById(cmd.id());
        if (maybe.isEmpty()) {
            Notification n = Notification.empty();
            n.addError("PRODUCT_NOT_FOUND", "produto não encontrado");
            return Result.failure(n);
        }
        Product p = maybe.get();
        if (cmd.available()) p.markAvailable(); else p.markUnavailable();
        products.save(p);
        return Result.success(null);
    }
}
```

- [ ] **Step 12.5:** `SetProductStockUseCase`

```java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.SetProductStockCommand;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.model.Stock;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SetProductStockUseCase {
    private final ProductRepository products;
    public SetProductStockUseCase(ProductRepository products) { this.products = products; }

    @Transactional
    public Result<Void> execute(SetProductStockCommand cmd) {
        Notification n = Notification.empty();
        if (cmd.quantity() != null && cmd.quantity() < 0) {
            n.addError("quantity", "INVALID_STOCK", "estoque inválido");
            return Result.failure(n);
        }
        Optional<Product> maybe = products.findById(cmd.id());
        if (maybe.isEmpty()) {
            n.addError("PRODUCT_NOT_FOUND", "produto não encontrado");
            return Result.failure(n);
        }
        Product p = maybe.get();
        p.changeStock(cmd.quantity() == null ? Stock.untracked() : Stock.of(cmd.quantity()));
        products.save(p);
        return Result.success(null);
    }
}
```

- [ ] **Step 12.6:** `DeleteProductUseCase`

```java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteProductUseCase {
    private final ProductRepository products;
    public DeleteProductUseCase(ProductRepository products) { this.products = products; }

    @Transactional
    public Result<Void> execute(ProductId id) {
        if (products.findById(id).isEmpty()) {
            Notification n = Notification.empty();
            n.addError("PRODUCT_NOT_FOUND", "produto não encontrado");
            return Result.failure(n);
        }
        products.deleteById(id);
        return Result.success(null);
    }
}
```

- [ ] **Step 12.7:** Test

```java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.*;
import com.cardapio.catalog.domain.model.*;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Money;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductUseCasesTest {

    private final ProductRepository products = mock(ProductRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);

    @Test
    void createsProductWithVariationsAndAddOns() {
        when(categories.existsById(any())).thenReturn(true);

        var cmd = new CreateProductCommand(
            "Pizza", "Mussarela", Money.brl("39.90"),
            CategoryId.newId(), null, true,
            List.of(new CreateProductCommand.VariationDraft("M", Money.brl("0.00")),
                    new CreateProductCommand.VariationDraft("G", Money.brl("10.00"))),
            List.of(new CreateProductCommand.AddOnGroupDraft("Adicionais", 0, 3,
                List.of(new CreateProductCommand.AddOnItemDraft("Bacon", Money.brl("3.00"))))));

        Result<ProductId> r = new CreateProductUseCase(products, categories).execute(cmd);
        assertThat(r.isSuccess()).isTrue();
        verify(products).save(any(Product.class));
    }

    @Test
    void rejectsCreateForUnknownCategory() {
        when(categories.existsById(any())).thenReturn(false);
        var cmd = new CreateProductCommand("X", "y", Money.brl("1.00"),
            CategoryId.newId(), null, false, List.of(), List.of());
        Result<ProductId> r = new CreateProductUseCase(products, categories).execute(cmd);
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    void togglesAvailability() {
        ProductId id = ProductId.newId();
        Product p = Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false);
        when(products.findById(id)).thenReturn(Optional.of(p));

        new SetProductAvailabilityUseCase(products).execute(new SetProductAvailabilityCommand(id, false));
        assertThat(p.isAvailable()).isFalse();
        verify(products).save(p);
    }

    @Test
    void setStockToTracked() {
        ProductId id = ProductId.newId();
        Product p = Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false);
        when(products.findById(id)).thenReturn(Optional.of(p));

        new SetProductStockUseCase(products).execute(new SetProductStockCommand(id, 25));
        assertThat(p.stock().quantity()).isEqualTo(25);
    }

    @Test
    void setStockToUntracked() {
        ProductId id = ProductId.newId();
        Product p = Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false);
        p.changeStock(Stock.of(10));
        when(products.findById(id)).thenReturn(Optional.of(p));

        new SetProductStockUseCase(products).execute(new SetProductStockCommand(id, null));
        assertThat(p.stock().isTracked()).isFalse();
    }

    @Test
    void deleteExistingProduct() {
        ProductId id = ProductId.newId();
        when(products.findById(id)).thenReturn(Optional.of(
            Product.create("X", "y", Money.brl("1.00"), CategoryId.newId(), null, false)));
        Result<Void> r = new DeleteProductUseCase(products).execute(id);
        assertThat(r.isSuccess()).isTrue();
        verify(products).deleteById(id);
    }
}
```

- [ ] **Step 12.8:** Run, pass, commit

```bash
./mvnw test -Dtest=ProductUseCasesTest
git add src/main/java/com/cardapio/catalog/application src/test/java/com/cardapio/catalog/application/usecase/ProductUseCasesTest.java
git commit -m "feat(catalog): add Product CRUD + availability + stock use cases"
```

---

## Task 13: OperatingHours use case + public queries

**Files:**
- `src/main/java/com/cardapio/catalog/application/command/UpdateOperatingHoursCommand.java`
- `src/main/java/com/cardapio/catalog/application/usecase/UpdateOperatingHoursUseCase.java`
- `src/main/java/com/cardapio/catalog/application/dto/*` (views)
- `src/main/java/com/cardapio/catalog/application/usecase/GetMenuQuery.java`
- `src/main/java/com/cardapio/catalog/application/usecase/GetProductDetailsQuery.java`
- `src/main/java/com/cardapio/catalog/application/usecase/GetOperatingHoursQuery.java`
- Test: `src/test/java/com/cardapio/catalog/application/usecase/MenuQueryTest.java`

- [ ] **Step 13.1:** `UpdateOperatingHoursCommand` + use case

```java
// UpdateOperatingHoursCommand.java
package com.cardapio.catalog.application.command;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record UpdateOperatingHoursCommand(Map<DayOfWeek, List<TimeRangeDraft>> hoursByDay) {
    public record TimeRangeDraft(LocalTime openTime, LocalTime closeTime) {}
}
```

```java
// UpdateOperatingHoursUseCase.java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.UpdateOperatingHoursCommand;
import com.cardapio.catalog.domain.model.OperatingHours;
import com.cardapio.catalog.domain.model.TimeRange;
import com.cardapio.catalog.domain.port.OperatingHoursRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateOperatingHoursUseCase {
    private final OperatingHoursRepository repo;
    public UpdateOperatingHoursUseCase(OperatingHoursRepository repo) { this.repo = repo; }

    @Transactional
    public Result<Void> execute(UpdateOperatingHoursCommand cmd) {
        try {
            OperatingHours hours = repo.load().orElseGet(OperatingHours::empty);
            cmd.hoursByDay().forEach((day, ranges) -> {
                hours.setHoursFor(day, ranges.stream()
                    .map(r -> TimeRange.of(r.openTime(), r.closeTime()))
                    .toList());
            });
            repo.save(hours);
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            Notification n = Notification.empty();
            n.addError("INVALID_HOURS", e.getMessage());
            return Result.failure(n);
        }
    }
}
```

- [ ] **Step 13.2:** Application DTOs (views)

```java
// MenuView.java
package com.cardapio.catalog.application.dto;

import java.util.List;

public record MenuView(List<CategoryView> categories) {}
```

```java
// CategoryView.java
package com.cardapio.catalog.application.dto;

import com.cardapio.catalog.domain.model.CategoryId;

import java.util.List;

public record CategoryView(CategoryId id, String name, int displayOrder, List<ProductSummaryView> products) {}
```

```java
// ProductSummaryView.java
package com.cardapio.catalog.application.dto;

import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.shared.domain.Money;

public record ProductSummaryView(ProductId id, String name, String description, Money basePrice, String imageUrl) {}
```

```java
// ProductDetailsView.java
package com.cardapio.catalog.application.dto;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.shared.domain.Money;

import java.util.List;

public record ProductDetailsView(
    ProductId id,
    CategoryId categoryId,
    String name,
    String description,
    Money basePrice,
    String imageUrl,
    boolean available,
    boolean allowsHalfHalf,
    Integer stockQuantity,        // null = untracked
    List<VariationView> variations,
    List<AddOnGroupView> addOnGroups
) {}
```

```java
// VariationView.java
package com.cardapio.catalog.application.dto;

import com.cardapio.catalog.domain.model.VariationId;
import com.cardapio.shared.domain.Money;

public record VariationView(VariationId id, String name, Money priceModifier) {}
```

```java
// AddOnGroupView.java
package com.cardapio.catalog.application.dto;

import com.cardapio.catalog.domain.model.AddOnGroupId;

import java.util.List;

public record AddOnGroupView(AddOnGroupId id, String name, int minSelection, int maxSelection, List<AddOnItemView> items) {}
```

```java
// AddOnItemView.java
package com.cardapio.catalog.application.dto;

import com.cardapio.catalog.domain.model.AddOnItemId;
import com.cardapio.shared.domain.Money;

public record AddOnItemView(AddOnItemId id, String name, Money price) {}
```

```java
// OperatingHoursView.java
package com.cardapio.catalog.application.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record OperatingHoursView(Map<DayOfWeek, List<TimeRangeView>> hoursByDay) {
    public record TimeRangeView(LocalTime openTime, LocalTime closeTime) {}
}
```

- [ ] **Step 13.3:** Public query use cases

```java
// GetMenuQuery.java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.dto.CategoryView;
import com.cardapio.catalog.application.dto.MenuView;
import com.cardapio.catalog.application.dto.ProductSummaryView;
import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetMenuQuery {
    private final CategoryRepository categories;
    private final ProductRepository products;

    public GetMenuQuery(CategoryRepository categories, ProductRepository products) {
        this.categories = categories; this.products = products;
    }

    @Transactional(readOnly = true)
    public MenuView execute() {
        List<Category> active = categories.findAllActive();
        List<CategoryView> views = active.stream().map(c -> {
            List<Product> ps = products.findAvailableByCategory(c.id());
            List<ProductSummaryView> productViews = ps.stream()
                .map(p -> new ProductSummaryView(p.id(), p.name(), p.description(), p.basePrice(), p.imageUrl()))
                .toList();
            return new CategoryView(c.id(), c.name(), c.displayOrder(), productViews);
        }).toList();
        return new MenuView(views);
    }
}
```

```java
// GetProductDetailsQuery.java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.dto.*;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class GetProductDetailsQuery {
    private final ProductRepository products;
    public GetProductDetailsQuery(ProductRepository products) { this.products = products; }

    @Transactional(readOnly = true)
    public Result<ProductDetailsView> execute(ProductId id) {
        Optional<Product> maybe = products.findById(id);
        if (maybe.isEmpty()) {
            Notification n = Notification.empty();
            n.addError("PRODUCT_NOT_FOUND", "produto não encontrado");
            return Result.failure(n);
        }
        Product p = maybe.get();
        var variations = p.variations().stream()
            .map(v -> new VariationView(v.id(), v.name(), v.priceModifier()))
            .toList();
        var groups = p.addOnGroups().stream()
            .map(g -> new AddOnGroupView(g.id(), g.name(), g.minSelection(), g.maxSelection(),
                g.items().stream().map(i -> new AddOnItemView(i.id(), i.name(), i.price())).toList()))
            .toList();
        return Result.success(new ProductDetailsView(
            p.id(), p.categoryId(), p.name(), p.description(), p.basePrice(), p.imageUrl(),
            p.isAvailable(), p.allowsHalfHalf(), p.stock().rawQuantity(), variations, groups));
    }
}
```

```java
// GetOperatingHoursQuery.java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.dto.OperatingHoursView;
import com.cardapio.catalog.domain.model.OperatingHours;
import com.cardapio.catalog.domain.port.OperatingHoursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class GetOperatingHoursQuery {
    private final OperatingHoursRepository repo;
    public GetOperatingHoursQuery(OperatingHoursRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public OperatingHoursView execute() {
        OperatingHours hours = repo.load().orElseGet(OperatingHours::empty);
        Map<DayOfWeek, List<OperatingHoursView.TimeRangeView>> result = new EnumMap<>(DayOfWeek.class);
        hours.snapshot().forEach((day, dh) -> {
            result.put(day, dh.intervals().stream()
                .map(r -> new OperatingHoursView.TimeRangeView(r.openTime(), r.closeTime()))
                .toList());
        });
        return new OperatingHoursView(result);
    }
}
```

- [ ] **Step 13.4:** Test for `GetMenuQuery`

```java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.dto.MenuView;
import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.Product;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.catalog.domain.port.ProductRepository;
import com.cardapio.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MenuQueryTest {

    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);

    @Test
    void returnsActiveCategoriesWithAvailableProducts() {
        CategoryId catId = CategoryId.newId();
        Category cat = Category.rehydrate(catId, "Pizzas", 1, true);
        Product pizza = Product.create("Margherita", "desc", Money.brl("39.90"), catId, null, false);

        when(categories.findAllActive()).thenReturn(List.of(cat));
        when(products.findAvailableByCategory(catId)).thenReturn(List.of(pizza));

        MenuView menu = new GetMenuQuery(categories, products).execute();
        assertThat(menu.categories()).hasSize(1);
        assertThat(menu.categories().get(0).products()).hasSize(1);
        assertThat(menu.categories().get(0).products().get(0).name()).isEqualTo("Margherita");
    }
}
```

- [ ] **Step 13.5:** Run, pass, commit

```bash
./mvnw test -Dtest=MenuQueryTest
git add src/main/java/com/cardapio/catalog/application src/test/java/com/cardapio/catalog/application/usecase/MenuQueryTest.java
git commit -m "feat(catalog): add OperatingHours update + public queries (menu, product details, hours)"
```

---

## Task 14: SecurityConfig — wire admin and public catalog paths

**Files:**
- Modify: `src/main/java/com/cardapio/identity/api/security/SecurityConfig.java`

- [ ] **Step 14.1:** Add catalog paths to security config

Replace the `authorizeHttpRequests` block in `SecurityConfig.filterChain` with:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/v1/menu/**", "/api/v1/operating-hours").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/v1/auth/register",
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/refresh",
                                        "/api/v1/admin/auth/login").permitAll()
    .requestMatchers("/api/v1/admin/**").hasAnyRole("OWNER", "MANAGER", "OPERATOR")
    .anyRequest().authenticated()
)
```

- [ ] **Step 14.2:** Run all tests to ensure nothing broke

Run: `./mvnw test`
Expected: PASS — Phase 1.B identity tests still work.

- [ ] **Step 14.3:** Commit

```bash
git add src/main/java/com/cardapio/identity/api/security/SecurityConfig.java
git commit -m "feat(security): permit public menu/operating-hours and require admin role on /admin/**"
```

---

## Task 15: Admin REST controllers

**Files:**
- API request/response DTOs under `src/main/java/com/cardapio/catalog/api/dto/`
- 3 controllers under `src/main/java/com/cardapio/catalog/api/rest/`:
  - `CategoryAdminController.java`
  - `ProductAdminController.java`
  - `OperatingHoursAdminController.java`

- [ ] **Step 15.1:** Request DTOs

```java
// CategoryRequest.java
package com.cardapio.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CategoryRequest(@NotBlank String name, @PositiveOrZero int displayOrder, Boolean active) {}
```

```java
// CategoryResponse.java
package com.cardapio.catalog.api.dto;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, int displayOrder, boolean active) {}
```

```java
// VariationRequest.java
package com.cardapio.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record VariationRequest(@NotBlank String name, @NotNull BigDecimal priceModifier) {}
```

```java
// AddOnItemRequest.java
package com.cardapio.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddOnItemRequest(@NotBlank String name, @NotNull BigDecimal price) {}
```

```java
// AddOnGroupRequest.java
package com.cardapio.catalog.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record AddOnGroupRequest(
    @NotBlank String name,
    @PositiveOrZero int minSelection,
    @PositiveOrZero int maxSelection,
    @NotNull @Valid List<AddOnItemRequest> items
) {}
```

```java
// ProductRequest.java
package com.cardapio.catalog.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductRequest(
    @NotBlank String name,
    String description,
    @NotNull @Positive BigDecimal basePrice,
    @NotNull UUID categoryId,
    String imageUrl,
    boolean allowsHalfHalf,
    @Valid List<VariationRequest> variations,
    @Valid List<AddOnGroupRequest> addOnGroups
) {}
```

```java
// SetAvailabilityRequest.java
package com.cardapio.catalog.api.dto;

public record SetAvailabilityRequest(boolean available) {}
```

```java
// SetStockRequest.java
package com.cardapio.catalog.api.dto;

public record SetStockRequest(Integer quantity) {}
```

```java
// OperatingHoursRequest.java
package com.cardapio.catalog.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record OperatingHoursRequest(@NotNull Map<DayOfWeek, List<TimeRangeDto>> hoursByDay) {
    public record TimeRangeDto(@NotNull LocalTime openTime, @NotNull LocalTime closeTime) {}
}
```

- [ ] **Step 15.2:** `CategoryAdminController`

```java
package com.cardapio.catalog.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.catalog.api.dto.CategoryRequest;
import com.cardapio.catalog.api.dto.CategoryResponse;
import com.cardapio.catalog.application.command.CreateCategoryCommand;
import com.cardapio.catalog.application.command.UpdateCategoryCommand;
import com.cardapio.catalog.application.usecase.CreateCategoryUseCase;
import com.cardapio.catalog.application.usecase.DeleteCategoryUseCase;
import com.cardapio.catalog.application.usecase.UpdateCategoryUseCase;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.shared.domain.Result;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class CategoryAdminController {

    private final CategoryRepository repo;
    private final CreateCategoryUseCase create;
    private final UpdateCategoryUseCase update;
    private final DeleteCategoryUseCase delete;

    public CategoryAdminController(CategoryRepository repo, CreateCategoryUseCase create,
                                   UpdateCategoryUseCase update, DeleteCategoryUseCase delete) {
        this.repo = repo; this.create = create; this.update = update; this.delete = delete;
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return repo.findAll().stream()
            .map(c -> new CategoryResponse(c.id().value(), c.name(), c.displayOrder(), c.isActive()))
            .toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CategoryRequest req) {
        Result<CategoryId> r = create.execute(new CreateCategoryCommand(req.name(), req.displayOrder()));
        return switch (r) {
            case Result.Success<CategoryId> s -> ResponseEntity.created(URI.create("/api/v1/admin/categories/" + s.value().value()))
                .body(new CategoryResponse(s.value().value(), req.name(), req.displayOrder(), true));
            case Result.Failure<CategoryId> f -> unprocessable(f);
        };
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest req) {
        boolean active = req.active() == null ? true : req.active();
        Result<CategoryId> r = update.execute(new UpdateCategoryCommand(CategoryId.of(id), req.name(), req.displayOrder(), active));
        return switch (r) {
            case Result.Success<CategoryId> s -> ResponseEntity.ok(new CategoryResponse(id, req.name(), req.displayOrder(), active));
            case Result.Failure<CategoryId> f -> unprocessable(f);
        };
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        Result<Void> r = delete.execute(CategoryId.of(id));
        return switch (r) {
            case Result.Success<Void> s -> ResponseEntity.noContent().build();
            case Result.Failure<Void> f -> unprocessable(f);
        };
    }

    private ResponseEntity<ProblemDetail> unprocessable(Result.Failure<?> f) {
        return ResponseEntity.unprocessableEntity()
            .contentType(MediaType.parseMediaType("application/problem+json"))
            .body(ProblemDetails.fromNotification(f.notification()));
    }
}
```

- [ ] **Step 15.3:** `ProductAdminController`

```java
package com.cardapio.catalog.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.catalog.api.dto.*;
import com.cardapio.catalog.application.command.*;
import com.cardapio.catalog.application.usecase.*;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.shared.domain.Money;
import com.cardapio.shared.domain.Result;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class ProductAdminController {

    private final CreateProductUseCase create;
    private final UpdateProductUseCase update;
    private final DeleteProductUseCase delete;
    private final SetProductAvailabilityUseCase setAvailability;
    private final SetProductStockUseCase setStock;

    public ProductAdminController(CreateProductUseCase create, UpdateProductUseCase update, DeleteProductUseCase delete,
                                  SetProductAvailabilityUseCase setAvailability, SetProductStockUseCase setStock) {
        this.create = create; this.update = update; this.delete = delete;
        this.setAvailability = setAvailability; this.setStock = setStock;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ProductRequest req) {
        var cmd = toCreateCommand(req);
        Result<ProductId> r = create.execute(cmd);
        return switch (r) {
            case Result.Success<ProductId> s -> ResponseEntity.created(URI.create("/api/v1/admin/products/" + s.value().value()))
                .body(java.util.Map.of("id", s.value().value()));
            case Result.Failure<ProductId> f -> unprocessable(f);
        };
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest req) {
        var cmd = new UpdateProductCommand(
            ProductId.of(id), req.name(), req.description() == null ? "" : req.description(),
            Money.brl(req.basePrice().toPlainString()), CategoryId.of(req.categoryId()),
            req.imageUrl(), req.allowsHalfHalf(),
            mapVariations(req.variations()), mapAddOns(req.addOnGroups()));
        Result<ProductId> r = update.execute(cmd);
        return switch (r) {
            case Result.Success<ProductId> s -> ResponseEntity.ok(java.util.Map.of("id", id));
            case Result.Failure<ProductId> f -> unprocessable(f);
        };
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<?> availability(@PathVariable UUID id, @RequestBody SetAvailabilityRequest req) {
        Result<Void> r = setAvailability.execute(new SetProductAvailabilityCommand(ProductId.of(id), req.available()));
        return switch (r) {
            case Result.Success<Void> s -> ResponseEntity.noContent().build();
            case Result.Failure<Void> f -> unprocessable(f);
        };
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> stock(@PathVariable UUID id, @RequestBody SetStockRequest req) {
        Result<Void> r = setStock.execute(new SetProductStockCommand(ProductId.of(id), req.quantity()));
        return switch (r) {
            case Result.Success<Void> s -> ResponseEntity.noContent().build();
            case Result.Failure<Void> f -> unprocessable(f);
        };
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        Result<Void> r = delete.execute(ProductId.of(id));
        return switch (r) {
            case Result.Success<Void> s -> ResponseEntity.noContent().build();
            case Result.Failure<Void> f -> unprocessable(f);
        };
    }

    private CreateProductCommand toCreateCommand(ProductRequest req) {
        return new CreateProductCommand(req.name(), req.description() == null ? "" : req.description(),
            Money.brl(req.basePrice().toPlainString()), CategoryId.of(req.categoryId()),
            req.imageUrl(), req.allowsHalfHalf(),
            mapVariations(req.variations()), mapAddOns(req.addOnGroups()));
    }

    private List<CreateProductCommand.VariationDraft> mapVariations(List<VariationRequest> vs) {
        if (vs == null) return List.of();
        return vs.stream()
            .map(v -> new CreateProductCommand.VariationDraft(v.name(), Money.brl(v.priceModifier().toPlainString())))
            .toList();
    }

    private List<CreateProductCommand.AddOnGroupDraft> mapAddOns(List<AddOnGroupRequest> gs) {
        if (gs == null) return List.of();
        return gs.stream()
            .map(g -> new CreateProductCommand.AddOnGroupDraft(g.name(), g.minSelection(), g.maxSelection(),
                g.items().stream().map(i -> new CreateProductCommand.AddOnItemDraft(i.name(), Money.brl(i.price().toPlainString()))).toList()))
            .toList();
    }

    private ResponseEntity<ProblemDetail> unprocessable(Result.Failure<?> f) {
        return ResponseEntity.unprocessableEntity()
            .contentType(MediaType.parseMediaType("application/problem+json"))
            .body(ProblemDetails.fromNotification(f.notification()));
    }
}
```

- [ ] **Step 15.4:** `OperatingHoursAdminController`

```java
package com.cardapio.catalog.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.catalog.api.dto.OperatingHoursRequest;
import com.cardapio.catalog.application.command.UpdateOperatingHoursCommand;
import com.cardapio.catalog.application.usecase.UpdateOperatingHoursUseCase;
import com.cardapio.shared.domain.Result;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/operating-hours")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class OperatingHoursAdminController {

    private final UpdateOperatingHoursUseCase update;

    public OperatingHoursAdminController(UpdateOperatingHoursUseCase update) { this.update = update; }

    @PutMapping
    public ResponseEntity<?> updateAll(@Valid @RequestBody OperatingHoursRequest req) {
        Map<DayOfWeek, List<UpdateOperatingHoursCommand.TimeRangeDraft>> map = req.hoursByDay().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                .map(t -> new UpdateOperatingHoursCommand.TimeRangeDraft(t.openTime(), t.closeTime()))
                .toList()));
        Result<Void> r = update.execute(new UpdateOperatingHoursCommand(map));
        return switch (r) {
            case Result.Success<Void> s -> ResponseEntity.noContent().build();
            case Result.Failure<Void> f -> ResponseEntity.unprocessableEntity()
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body((ProblemDetail) ProblemDetails.fromNotification(f.notification()));
        };
    }
}
```

- [ ] **Step 15.5:** Run tests, commit

Run: `./mvnw test`
Expected: PASS.

```bash
git add src/main/java/com/cardapio/catalog/api
git commit -m "feat(catalog): add admin REST controllers for categories, products, operating-hours"
```

---

## Task 16: Public REST controllers + E2E test

**Files:**
- `src/main/java/com/cardapio/catalog/api/rest/PublicMenuController.java`
- `src/main/java/com/cardapio/catalog/api/rest/PublicOperatingHoursController.java`
- `src/main/java/com/cardapio/catalog/api/dto/MenuResponse.java` (and friends)
- Test: `src/test/java/com/cardapio/catalog/api/CatalogE2ETest.java`

- [ ] **Step 16.1:** Response DTOs for public endpoints

```java
// MenuResponse.java
package com.cardapio.catalog.api.dto;

import com.cardapio.catalog.application.dto.MenuView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MenuResponse(List<CategoryDto> categories) {

    public record CategoryDto(UUID id, String name, int displayOrder, List<ProductDto> products) {}
    public record ProductDto(UUID id, String name, String description, BigDecimal basePrice, String imageUrl) {}

    public static MenuResponse from(MenuView v) {
        var cats = v.categories().stream()
            .map(c -> new CategoryDto(c.id().value(), c.name(), c.displayOrder(),
                c.products().stream()
                    .map(p -> new ProductDto(p.id().value(), p.name(), p.description(), p.basePrice().amount(), p.imageUrl()))
                    .toList()))
            .toList();
        return new MenuResponse(cats);
    }
}
```

```java
// ProductDetailsResponse.java
package com.cardapio.catalog.api.dto;

import com.cardapio.catalog.application.dto.ProductDetailsView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductDetailsResponse(
    UUID id, UUID categoryId, String name, String description, BigDecimal basePrice,
    String imageUrl, boolean available, boolean allowsHalfHalf, Integer stockQuantity,
    List<VariationDto> variations, List<AddOnGroupDto> addOnGroups
) {
    public record VariationDto(UUID id, String name, BigDecimal priceModifier) {}
    public record AddOnGroupDto(UUID id, String name, int minSelection, int maxSelection, List<AddOnItemDto> items) {}
    public record AddOnItemDto(UUID id, String name, BigDecimal price) {}

    public static ProductDetailsResponse from(ProductDetailsView v) {
        return new ProductDetailsResponse(
            v.id().value(), v.categoryId().value(), v.name(), v.description(), v.basePrice().amount(),
            v.imageUrl(), v.available(), v.allowsHalfHalf(), v.stockQuantity(),
            v.variations().stream().map(va -> new VariationDto(va.id().value(), va.name(), va.priceModifier().amount())).toList(),
            v.addOnGroups().stream().map(g -> new AddOnGroupDto(g.id().value(), g.name(), g.minSelection(), g.maxSelection(),
                g.items().stream().map(i -> new AddOnItemDto(i.id().value(), i.name(), i.price().amount())).toList()))
                .toList());
    }
}
```

```java
// OperatingHoursResponse.java
package com.cardapio.catalog.api.dto;

import com.cardapio.catalog.application.dto.OperatingHoursView;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record OperatingHoursResponse(Map<DayOfWeek, List<TimeRangeDto>> hoursByDay) {
    public record TimeRangeDto(LocalTime openTime, LocalTime closeTime) {}

    public static OperatingHoursResponse from(OperatingHoursView v) {
        return new OperatingHoursResponse(v.hoursByDay().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().stream().map(t -> new TimeRangeDto(t.openTime(), t.closeTime())).toList())));
    }
}
```

- [ ] **Step 16.2:** `PublicMenuController`

```java
package com.cardapio.catalog.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.catalog.api.dto.MenuResponse;
import com.cardapio.catalog.api.dto.ProductDetailsResponse;
import com.cardapio.catalog.application.dto.ProductDetailsView;
import com.cardapio.catalog.application.usecase.GetMenuQuery;
import com.cardapio.catalog.application.usecase.GetProductDetailsQuery;
import com.cardapio.catalog.domain.model.ProductId;
import com.cardapio.shared.domain.Result;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menu")
public class PublicMenuController {

    private final GetMenuQuery menu;
    private final GetProductDetailsQuery details;

    public PublicMenuController(GetMenuQuery menu, GetProductDetailsQuery details) {
        this.menu = menu; this.details = details;
    }

    @GetMapping
    public MenuResponse menu() {
        return MenuResponse.from(menu.execute());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> product(@PathVariable UUID id) {
        Result<ProductDetailsView> r = details.execute(ProductId.of(id));
        return switch (r) {
            case Result.Success<ProductDetailsView> s -> ResponseEntity.ok(ProductDetailsResponse.from(s.value()));
            case Result.Failure<ProductDetailsView> f -> ResponseEntity.status(404)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetails.fromNotification(f.notification()));
        };
    }
}
```

- [ ] **Step 16.3:** `PublicOperatingHoursController`

```java
package com.cardapio.catalog.api.rest;

import com.cardapio.catalog.api.dto.OperatingHoursResponse;
import com.cardapio.catalog.application.usecase.GetOperatingHoursQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operating-hours")
public class PublicOperatingHoursController {

    private final GetOperatingHoursQuery query;
    public PublicOperatingHoursController(GetOperatingHoursQuery query) { this.query = query; }

    @GetMapping
    public OperatingHoursResponse get() {
        return OperatingHoursResponse.from(query.execute());
    }
}
```

- [ ] **Step 16.4:** E2E test (creates category + product as admin, fetches public menu as anonymous)

```java
package com.cardapio.catalog.api;

import com.cardapio.support.PostgresTestContainerConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class CatalogE2ETest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    @WithMockUser(roles = {"OWNER"})
    void adminCreatesCategoryAndProduct_publicSeesMenu() throws Exception {
        // 1. Create category
        String catBody = """
            { "name": "Pizzas", "displayOrder": 1, "active": true }
            """;
        MvcResult catResult = mvc.perform(post("/api/v1/admin/categories")
                .contentType(MediaType.APPLICATION_JSON).content(catBody))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode cat = json.readTree(catResult.getResponse().getContentAsString());
        String categoryId = cat.get("id").asText();

        // 2. Create product
        String prodBody = """
            {
              "name": "Pizza Margherita",
              "description": "Molho, mussarela, manjericão",
              "basePrice": 39.90,
              "categoryId": "%s",
              "imageUrl": null,
              "allowsHalfHalf": true,
              "variations": [
                {"name": "Pequena", "priceModifier": 0.00},
                {"name": "Grande", "priceModifier": 10.00}
              ],
              "addOnGroups": [
                {"name": "Adicionais", "minSelection": 0, "maxSelection": 3,
                 "items": [{"name": "Bacon", "price": 3.00}]}
              ]
            }
            """.formatted(categoryId);
        mvc.perform(post("/api/v1/admin/products")
                .contentType(MediaType.APPLICATION_JSON).content(prodBody))
            .andExpect(status().isCreated());
    }

    @Test
    void publicMenuIsAnonymous() throws Exception {
        mvc.perform(get("/api/v1/menu"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categories").isArray());
    }

    @Test
    void publicOperatingHoursIsAnonymous() throws Exception {
        mvc.perform(get("/api/v1/operating-hours"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hoursByDay").exists());
    }

    @Test
    void adminEndpointRequiresAuth() throws Exception {
        mvc.perform(get("/api/v1/admin/categories"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"OPERATOR"})
    void operatorCannotManageCategories() throws Exception {
        mvc.perform(post("/api/v1/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"displayOrder\":1,\"active\":true}"))
            .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 16.5:** Run all tests

Run: `./mvnw test`
Expected: PASS — all 5 catalog E2E tests + Phase 1.A/1.B tests.

- [ ] **Step 16.6:** Commit

```bash
git add src/main/java/com/cardapio/catalog/api/rest/PublicMenuController.java \
        src/main/java/com/cardapio/catalog/api/rest/PublicOperatingHoursController.java \
        src/main/java/com/cardapio/catalog/api/dto/MenuResponse.java \
        src/main/java/com/cardapio/catalog/api/dto/ProductDetailsResponse.java \
        src/main/java/com/cardapio/catalog/api/dto/OperatingHoursResponse.java \
        src/test/java/com/cardapio/catalog/api/CatalogE2ETest.java
git commit -m "feat(catalog): add public menu/product/operating-hours endpoints + E2E test"
```

---

## Task 17: Final verification + tag

- [ ] **Step 17.1:** `./mvnw clean verify`

Expected: BUILD SUCCESS. All tests pass.

- [ ] **Step 17.2:** Modulith verifier

Run: `./mvnw test -Dtest=ModulithVerificationTest`
Expected: PASS — `catalog` module respects `allowedDependencies = "shared"`.

- [ ] **Step 17.3:** ArchUnit

Run: `./mvnw test -Dtest=CleanArchitectureTest`
Expected: PASS — domain layer has no Spring/JPA imports.

- [ ] **Step 17.4:** Tag

```bash
git tag -a phase-1c-catalog -m "Phase 1.C complete: Catalog (Category, Product, OperatingHours)"
```

---

## Self-Review Checklist

- [ ] All 17 tasks committed
- [ ] `./mvnw clean verify` exits 0
- [ ] `catalog` module passes Modulith verifier with `allowedDependencies = "shared"` only
- [ ] Domain code has no Spring/JPA imports (CleanArchitectureTest)
- [ ] V4 migration applied; all 6 catalog tables present
- [ ] Public endpoints (`/menu`, `/menu/products/{id}`, `/operating-hours`) work without auth
- [ ] Admin endpoints require role `OWNER` or `MANAGER`
- [ ] CatalogE2ETest covers admin → product creation, public listing, role enforcement

---

## Definition of Done — Phase 1.C

- ✅ `catalog` Modulith module with `allowedDependencies = {"shared"}`
- ✅ Aggregates: Category, Product (with Variation + AddOnGroup + AddOnItem entities), OperatingHours
- ✅ VOs: 5 typed IDs, Stock (tracked/untracked), TimeRange (validates close > open)
- ✅ Domain ports: CategoryRepository, ProductRepository, OperatingHoursRepository
- ✅ Flyway V4 migration with cascading FK constraints
- ✅ JPA persistence with one-to-many cascade on Product children
- ✅ Use cases: Category CRUD, Product CRUD + availability + stock, OperatingHours update, public queries (menu, product details, operating hours)
- ✅ Admin REST endpoints with `@PreAuthorize("hasAnyRole('OWNER','MANAGER')")`
- ✅ Public REST endpoints anonymous via SecurityConfig
- ✅ E2E test: admin creates → public sees + role enforcement

**Next plan (Phase 2):** Cart + Order (delivery + retirada modalities) — first chunk of the ordering context.
