package com.cardapio.ordering.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_item_addons")
public class OrderItemAddOnJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "order_item_id", nullable = false) private UUID orderItemId;
    @Column(name = "addon_group_id", nullable = false) private UUID addOnGroupId;
    @Column(name = "addon_item_id", nullable = false) private UUID addOnItemId;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false) private int quantity;
    @Column(nullable = false) private int position;

    protected OrderItemAddOnJpaEntity() {}

    public OrderItemAddOnJpaEntity(UUID id, UUID orderItemId, UUID addOnGroupId, UUID addOnItemId, String name,
                                   BigDecimal price, String currency, int quantity, int position) {
        this.id = id; this.orderItemId = orderItemId; this.addOnGroupId = addOnGroupId; this.addOnItemId = addOnItemId;
        this.name = name; this.price = price; this.currency = currency;
        this.quantity = quantity; this.position = position;
    }

    public UUID getId() { return id; }
    public UUID getAddOnGroupId() { return addOnGroupId; }
    public UUID getAddOnItemId() { return addOnItemId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getCurrency() { return currency; }
    public int getQuantity() { return quantity; }
    public int getPosition() { return position; }
}
