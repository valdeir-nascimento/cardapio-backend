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
