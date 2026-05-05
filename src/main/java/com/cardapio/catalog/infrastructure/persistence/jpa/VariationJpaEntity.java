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
