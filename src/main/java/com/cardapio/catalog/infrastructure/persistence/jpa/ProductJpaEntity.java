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
