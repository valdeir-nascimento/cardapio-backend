package com.cardapio.delivery.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "neighborhoods")
public class NeighborhoodJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, length = 120) private String city;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal fee;
    @Column(nullable = false, length = 3) private String currency;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected NeighborhoodJpaEntity() {}

    public NeighborhoodJpaEntity(UUID id, String name, String city, BigDecimal fee, String currency,
                                 boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id; this.name = name; this.city = city; this.fee = fee; this.currency = currency;
        this.active = active; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public BigDecimal getFee() { return fee; }
    public String getCurrency() { return currency; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setCity(String city) { this.city = city; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setActive(boolean active) { this.active = active; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
