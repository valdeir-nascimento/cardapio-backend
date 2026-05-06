package com.cardapio.ordering.infrastructure.persistence.jpa;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "comandas")
public class ComandaJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "table_id", nullable = false) private UUID tableId;
    @Column(nullable = false, length = 10) private String status;
    @Column(name = "opened_at", nullable = false) private Instant openedAt;
    @Column(name = "closed_at") private Instant closedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "comanda_customers", joinColumns = @JoinColumn(name = "comanda_id"))
    @Column(name = "customer_id", nullable = false)
    private Set<UUID> customerIds = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "comanda_orders", joinColumns = @JoinColumn(name = "comanda_id"))
    @OrderColumn(name = "position")
    @Column(name = "order_id", nullable = false)
    private List<UUID> orderIds = new ArrayList<>();

    protected ComandaJpaEntity() {}

    public ComandaJpaEntity(UUID id, UUID tableId, String status, Instant openedAt,
                            Instant closedAt, Instant updatedAt,
                            Set<UUID> customerIds, List<UUID> orderIds) {
        this.id = id;
        this.tableId = tableId;
        this.status = status;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.updatedAt = updatedAt;
        this.customerIds = new LinkedHashSet<>(customerIds);
        this.orderIds = new ArrayList<>(orderIds);
    }

    public UUID getId() { return id; }
    public UUID getTableId() { return tableId; }
    public String getStatus() { return status; }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getClosedAt() { return closedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Set<UUID> getCustomerIds() { return customerIds; }
    public List<UUID> getOrderIds() { return orderIds; }

    public void setStatus(String status) { this.status = status; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setCustomerIds(Set<UUID> customerIds) { this.customerIds = new LinkedHashSet<>(customerIds); }
    public void setOrderIds(List<UUID> orderIds) { this.orderIds = new ArrayList<>(orderIds); }
}
