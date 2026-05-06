package com.cardapio.ordering.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItemJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "product_name", nullable = false, length = 180) private String productName;

    @Column(name = "variation_id") private UUID variationId;
    @Column(name = "variation_name", length = 80) private String variationName;
    @Column(name = "variation_modifier", precision = 12, scale = 2) private BigDecimal variationModifier;

    @Column(name = "half_left_product_id") private UUID halfLeftProductId;
    @Column(name = "half_right_product_id") private UUID halfRightProductId;
    @Column(name = "half_display_name", length = 180) private String halfDisplayName;
    @Column(name = "half_base_price", precision = 12, scale = 2) private BigDecimal halfBasePrice;

    @Column(nullable = false, length = 200) private String observation;
    @Column(nullable = false) private int quantity;
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2) private BigDecimal lineTotal;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false) private int position;

    @OneToMany(mappedBy = "orderItemId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<OrderItemAddOnJpaEntity> addOns = new ArrayList<>();

    protected OrderItemJpaEntity() {}

    public OrderItemJpaEntity(UUID id, UUID orderId, UUID productId, String productName,
                              UUID variationId, String variationName, BigDecimal variationModifier,
                              UUID halfLeftProductId, UUID halfRightProductId, String halfDisplayName, BigDecimal halfBasePrice,
                              String observation, int quantity, BigDecimal lineTotal, String currency, int position) {
        this.id = id; this.orderId = orderId; this.productId = productId; this.productName = productName;
        this.variationId = variationId; this.variationName = variationName; this.variationModifier = variationModifier;
        this.halfLeftProductId = halfLeftProductId; this.halfRightProductId = halfRightProductId;
        this.halfDisplayName = halfDisplayName; this.halfBasePrice = halfBasePrice;
        this.observation = observation; this.quantity = quantity; this.lineTotal = lineTotal;
        this.currency = currency; this.position = position;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public UUID getVariationId() { return variationId; }
    public String getVariationName() { return variationName; }
    public BigDecimal getVariationModifier() { return variationModifier; }
    public UUID getHalfLeftProductId() { return halfLeftProductId; }
    public UUID getHalfRightProductId() { return halfRightProductId; }
    public String getHalfDisplayName() { return halfDisplayName; }
    public BigDecimal getHalfBasePrice() { return halfBasePrice; }
    public String getObservation() { return observation; }
    public int getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public String getCurrency() { return currency; }
    public int getPosition() { return position; }
    public List<OrderItemAddOnJpaEntity> getAddOns() { return addOns; }
}
