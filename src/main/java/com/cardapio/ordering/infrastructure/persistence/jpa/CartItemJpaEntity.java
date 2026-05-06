package com.cardapio.ordering.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItemJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "cart_id", nullable = false) private UUID cartId;
    @Column(name = "product_id", nullable = false) private UUID productId;

    @Column(name = "variation_id") private UUID variationId;
    @Column(name = "variation_name", length = 80) private String variationName;
    @Column(name = "variation_modifier", precision = 12, scale = 2) private BigDecimal variationModifier;

    @Column(name = "half_left_product_id") private UUID halfLeftProductId;
    @Column(name = "half_right_product_id") private UUID halfRightProductId;
    @Column(name = "half_display_name", length = 180) private String halfDisplayName;
    @Column(name = "half_base_price", precision = 12, scale = 2) private BigDecimal halfBasePrice;

    @Column(nullable = false, length = 200) private String observation;
    @Column(nullable = false) private int quantity;
    @Column(nullable = false) private int position;
    @Column(nullable = false, length = 3) private String currency;

    @OneToMany(mappedBy = "cartItemId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<CartItemAddOnJpaEntity> addOns = new ArrayList<>();

    protected CartItemJpaEntity() {}

    public CartItemJpaEntity(UUID id, UUID cartId, UUID productId, UUID variationId, String variationName, BigDecimal variationModifier,
                             UUID halfLeftProductId, UUID halfRightProductId, String halfDisplayName, BigDecimal halfBasePrice,
                             String observation, int quantity, int position, String currency) {
        this.id = id; this.cartId = cartId; this.productId = productId;
        this.variationId = variationId; this.variationName = variationName; this.variationModifier = variationModifier;
        this.halfLeftProductId = halfLeftProductId; this.halfRightProductId = halfRightProductId;
        this.halfDisplayName = halfDisplayName; this.halfBasePrice = halfBasePrice;
        this.observation = observation; this.quantity = quantity; this.position = position; this.currency = currency;
    }

    public UUID getId() { return id; }
    public UUID getCartId() { return cartId; }
    public UUID getProductId() { return productId; }
    public UUID getVariationId() { return variationId; }
    public String getVariationName() { return variationName; }
    public BigDecimal getVariationModifier() { return variationModifier; }
    public UUID getHalfLeftProductId() { return halfLeftProductId; }
    public UUID getHalfRightProductId() { return halfRightProductId; }
    public String getHalfDisplayName() { return halfDisplayName; }
    public BigDecimal getHalfBasePrice() { return halfBasePrice; }
    public String getObservation() { return observation; }
    public int getQuantity() { return quantity; }
    public int getPosition() { return position; }
    public String getCurrency() { return currency; }
    public List<CartItemAddOnJpaEntity> getAddOns() { return addOns; }
}
