package com.cardapio.ordering.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(nullable = false, length = 16) private String modality;
    @Column(nullable = false, length = 24) private String status;

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal;
    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2) private BigDecimal deliveryFee;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal discount;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total;
    @Column(nullable = false, length = 3) private String currency;

    @Column(name = "address_street", length = 160) private String addressStreet;
    @Column(name = "address_number", length = 20) private String addressNumber;
    @Column(name = "address_complement", length = 120) private String addressComplement;
    @Column(name = "address_district", length = 120) private String addressDistrict;
    @Column(name = "address_city", length = 120) private String addressCity;
    @Column(name = "address_postal_code", length = 16) private String addressPostalCode;
    @Column(name = "address_neighborhood_id") private UUID addressNeighborhoodId;

    @Column(name = "table_id") private UUID tableId;
    @Column(name = "comanda_id") private UUID comandaId;

    @Column(name = "placed_at", nullable = false) private Instant placedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @OneToMany(mappedBy = "orderId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    protected OrderJpaEntity() {}

    public OrderJpaEntity(UUID id, UUID customerId, String modality, String status,
                          BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal discount, BigDecimal total,
                          String currency,
                          String addressStreet, String addressNumber, String addressComplement, String addressDistrict,
                          String addressCity, String addressPostalCode, UUID addressNeighborhoodId,
                          UUID tableId, UUID comandaId,
                          Instant placedAt, Instant updatedAt) {
        this.id = id; this.customerId = customerId; this.modality = modality; this.status = status;
        this.subtotal = subtotal; this.deliveryFee = deliveryFee; this.discount = discount; this.total = total;
        this.currency = currency;
        this.addressStreet = addressStreet; this.addressNumber = addressNumber; this.addressComplement = addressComplement;
        this.addressDistrict = addressDistrict; this.addressCity = addressCity;
        this.addressPostalCode = addressPostalCode; this.addressNeighborhoodId = addressNeighborhoodId;
        this.tableId = tableId; this.comandaId = comandaId;
        this.placedAt = placedAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public String getModality() { return modality; }
    public String getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public String getCurrency() { return currency; }
    public String getAddressStreet() { return addressStreet; }
    public String getAddressNumber() { return addressNumber; }
    public String getAddressComplement() { return addressComplement; }
    public String getAddressDistrict() { return addressDistrict; }
    public String getAddressCity() { return addressCity; }
    public String getAddressPostalCode() { return addressPostalCode; }
    public UUID getAddressNeighborhoodId() { return addressNeighborhoodId; }
    public UUID getTableId() { return tableId; }
    public UUID getComandaId() { return comandaId; }
    public Instant getPlacedAt() { return placedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<OrderItemJpaEntity> getItems() { return items; }

    public void setStatus(String status) { this.status = status; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
