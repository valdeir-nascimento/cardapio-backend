package com.cardapio.ordering.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "tables")
public class TableJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(nullable = false) private Integer number;
    @Column(name = "qr_token", nullable = false) private UUID qrToken;
    @Column(name = "qr_image_key", length = 200) private String qrImageKey;
    @Column(nullable = false) private Boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected TableJpaEntity() {}

    public TableJpaEntity(UUID id, Integer number, UUID qrToken, String qrImageKey,
                          Boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.number = number;
        this.qrToken = qrToken;
        this.qrImageKey = qrImageKey;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public Integer getNumber() { return number; }
    public UUID getQrToken() { return qrToken; }
    public String getQrImageKey() { return qrImageKey; }
    public Boolean getActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setQrImageKey(String qrImageKey) { this.qrImageKey = qrImageKey; }
    public void setActive(Boolean active) { this.active = active; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
